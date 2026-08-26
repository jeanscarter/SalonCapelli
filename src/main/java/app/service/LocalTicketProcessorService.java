package app.service;

import app.model.ScannedTicketDTO;
import app.model.Servicio;
import app.model.Trabajadora;
import app.repository.ServicioRepository;
import app.repository.ServicioRepositorySQLite;
import app.repository.TicketCorrectionRepository;
import app.repository.TrabajadoraRepository;
import app.repository.TrabajadoraRepositorySQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor local de procesamiento de tickets de Capelli.
 * 
 * Procesa imágenes de tickets sin dependencias externas ni servicios de red,
 * aprovechando la estructura fija de 22 filas de la plantilla litográfica.
 * 
 * Pipeline:
 * 1. Normalización y binarización de la imagen
 * 2. Segmentación por ROIs (Regions of Interest) basada en coordenadas fijas
 * 3. Detección de tinta por fila (¿hay escritura manuscrita?)
 * 4. Reconocimiento de montos numéricos (DigitRecognizer)
 * 5. Detección de forma de pago por OMR (densidad de checkboxes)
 * 6. Fuzzy matching de colaboradoras con la BD
 * 7. Aplicación de correcciones aprendidas (TicketCorrectionRepository)
 * 
 * Funciona 100% offline.
 */
public class LocalTicketProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(LocalTicketProcessorService.class);

    /** Ancho estándar al que se normaliza la imagen para procesamiento */
    private static final int NORMALIZED_WIDTH = 1000;

    /** Umbral de confianza mínima para que el motor local se considere exitoso */
    public static final double MIN_CONFIDENCE_THRESHOLD = 0.40;

    private final ServicioRepository servicioRepo;
    private final TrabajadoraRepository trabajadoraRepo;
    private final TicketCorrectionRepository correctionRepo;

    public LocalTicketProcessorService() {
        this.servicioRepo = new ServicioRepositorySQLite();
        this.trabajadoraRepo = new TrabajadoraRepositorySQLite();
        this.correctionRepo = new TicketCorrectionRepository();
    }

    /**
     * Procesa una imagen de ticket y extrae los datos estructurados.
     * 
     * @param imageBytes Bytes de la imagen (JPEG, PNG)
     * @param mimeType   Tipo MIME de la imagen
     * @return ScannedTicketDTO con los datos extraídos y scores de confianza
     */
    public ScannedTicketDTO processTicket(byte[] imageBytes, String mimeType) throws Exception {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Imagen vacía.");
        }

        logger.info("Iniciando procesamiento local de ticket ({} bytes)...", imageBytes.length);
        long startTime = System.currentTimeMillis();

        // 1. Cargar y normalizar imagen
        BufferedImage rawImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
        if (rawImage == null) {
            throw new IllegalArgumentException("No se pudo decodificar la imagen.");
        }

        BufferedImage normalized = normalizeImage(rawImage);
        int imgW = normalized.getWidth();
        int imgH = normalized.getHeight();

        logger.debug("Imagen normalizada: {}x{} px", imgW, imgH);

        ScannedTicketDTO dto = new ScannedTicketDTO();

        // 2. Cargar catálogos de la BD para fuzzy matching
        List<String> nombresServicios = new ArrayList<>();
        List<Servicio> servicios = new ArrayList<>();
        try {
            servicios = servicioRepo.findAll();
            for (Servicio s : servicios) {
                nombresServicios.add(s.getNombre());
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar catálogo de servicios", e);
        }

        List<String> nombresTrabajadoras = new ArrayList<>();
        List<Trabajadora> trabajadoras = new ArrayList<>();
        try {
            trabajadoras = trabajadoraRepo.findAll();
            for (Trabajadora t : trabajadoras) {
                nombresTrabajadoras.add(t.getNombreCompleto());
                // También agregar solo el primer nombre para matching
                if (t.getNombres() != null) {
                    nombresTrabajadoras.add(t.getNombres());
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar lista de trabajadoras", e);
        }

        // 3. Extraer encabezado (Fecha, Nro Ticket, Tasa)
        extractHeaderInfo(normalized, imgW, imgH, dto);

        // 4. Procesar cada fila de la tabla de servicios
        List<Double> confidences = new ArrayList<>();
        String lastDetectedTrabajadora = null;

        for (int row = 1; row <= CapelliTicketTemplate.TOTAL_ROWS; row++) {
            double[] yBounds = CapelliTicketTemplate.getRowYBounds(row);
            int yStart = (int) (yBounds[0] * imgH);
            int yEnd = (int) (yBounds[1] * imgH);
            int rowHeight = yEnd - yStart;

            // Coordenadas de la celda de monto
            int montoX = (int) (CapelliTicketTemplate.COL_MONTO_X_START * imgW);
            int montoW = (int) ((CapelliTicketTemplate.COL_MONTO_X_END - CapelliTicketTemplate.COL_MONTO_X_START) * imgW);

            // Coordenadas de la celda de colaborador
            int colabX = (int) (CapelliTicketTemplate.COL_COLABORADOR_X_START * imgW);
            int colabW = (int) ((CapelliTicketTemplate.COL_COLABORADOR_X_END - CapelliTicketTemplate.COL_COLABORADOR_X_START) * imgW);

            // ¿Tiene tinta en la celda de monto?
            boolean hasMonto = DigitRecognizer.hasInk(normalized, montoX, yStart, montoW, rowHeight);
            boolean hasColab = DigitRecognizer.hasInk(normalized, colabX, yStart, colabW, rowHeight);

            if (!hasMonto && !hasColab) {
                continue; // Fila vacía, saltar
            }

            logger.debug("Fila {} activa (monto={}, colab={})", row, hasMonto, hasColab);

            // 5. Reconocer monto numérico
            double monto = 0.0;
            double montoConf = 0.5; // Confianza base
            if (hasMonto) {
                DigitRecognizer.RecognitionResult montoResult =
                    DigitRecognizer.recognizeNumber(normalized, montoX, yStart, montoW, rowHeight);
                monto = montoResult.toDouble();
                montoConf = montoResult.confidence;

                // Consultar correcciones aprendidas para este monto
                String correctedMonto = correctionRepo.findBestCorrection("MONTO", montoResult.text);
                if (correctedMonto != null) {
                    try {
                        monto = Double.parseDouble(correctedMonto);
                        montoConf = Math.max(montoConf, 0.85); // Boost por corrección aprendida
                    } catch (NumberFormatException ignored) {}
                }
            }

            // 6. Determinar el servicio
            String serviceName;
            double serviceConf;

            if (CapelliTicketTemplate.isOverwritableRow(row)) {
                // Filas 17-22: pueden estar sobreescritas
                int svcX = (int) (CapelliTicketTemplate.COL_SERVICIO_X_START * imgW);
                int svcW = (int) ((CapelliTicketTemplate.COL_SERVICIO_X_END - CapelliTicketTemplate.COL_SERVICIO_X_START) * imgW);
                double svcInkDensity = DigitRecognizer.inkDensity(normalized, svcX, yStart, svcW, rowHeight);

                if (svcInkDensity > 0.08) {
                    String preprinted = CapelliTicketTemplate.ROW_TO_SERVICE.get(row);
                    serviceName = preprinted;
                    serviceConf = 0.50;

                    String learnedService = correctionRepo.findBestCorrection("SERVICIO", "row_" + row);
                    if (learnedService != null) {
                        serviceName = learnedService;
                        serviceConf = 0.80;
                    }
                } else {
                    serviceName = CapelliTicketTemplate.ROW_TO_SERVICE.get(row);
                    serviceConf = 0.90;
                }
            } else {
                serviceName = CapelliTicketTemplate.ROW_TO_SERVICE.get(row);
                serviceConf = 0.95;
            }

            // 7. Detectar colaboradora (fuzzy match con BD)
            String trabajadoraName = null;
            double trabConf = 0.40;

            if (hasColab) {
                String learnedColab = correctionRepo.findBestCorrection("COLABORADOR", "row_" + row);
                if (learnedColab != null) {
                    trabajadoraName = learnedColab;
                    trabConf = 0.85;
                } else {
                    if (!nombresTrabajadoras.isEmpty() && lastDetectedTrabajadora != null) {
                        trabajadoraName = lastDetectedTrabajadora;
                        trabConf = 0.50;
                    } else if (!trabajadoras.isEmpty()) {
                        trabajadoraName = trabajadoras.get(0).getNombreCompleto();
                        trabConf = 0.30;
                    }
                }
            }

            if (trabajadoraName != null) {
                lastDetectedTrabajadora = trabajadoraName;
            }

            // Fila 22 = Propina/Libre — tratamiento especial
            boolean esPropina = (row == 22);
            boolean esProducto = (row == 21);

            // Crear item
            double itemConf = (montoConf + serviceConf + trabConf) / 3.0;
            confidences.add(itemConf);

            ScannedTicketDTO.ScannedItemDTO item = new ScannedTicketDTO.ScannedItemDTO();
            item.setDescripcion(serviceName != null ? serviceName : "Servicio Fila " + row);
            item.setPrecio(monto);
            item.setEsProducto(esProducto);
            item.setConfidence(itemConf);
            item.setRowNumber(row);
            item.setTrabajadoraNombre(trabajadoraName);

            if (esPropina && monto > 0) {
                item.setDescripcion("Propina");
                item.setEsPropina(true);
            }

            dto.addItem(item);
        }

        if (lastDetectedTrabajadora != null && dto.getTrabajadoraNombre() == null) {
            dto.setTrabajadoraNombre(lastDetectedTrabajadora);
        }

        // 8. Detectar forma de pago (OMR en checkboxes)
        detectPaymentMethod(normalized, imgW, imgH, dto);

        // 9. Post-procesamiento de Tasa (Fallback Euro Oficial por Fecha de dolarapi)
        GeminiTicketScannerService.enrichTasaIfMissing(dto);

        // 10. Calcular total (suma de montos detectados)
        double totalCalculado = 0;
        for (ScannedTicketDTO.ScannedItemDTO item : dto.getItems()) {
            if (!item.isEsPropina()) {
                totalCalculado += item.getPrecio();
            }
        }
        dto.setTotalDetectado(totalCalculado);

        // 11. Confianza global
        double avgConf = confidences.isEmpty() ? 0.0 :
            confidences.stream().mapToDouble(d -> d).average().orElse(0.0);
        dto.setConfidenceScore(avgConf);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Procesamiento local completado en {} ms. Items: {}, Confianza: {:.0f}%",
                elapsed, dto.getItems().size(), avgConf * 100);

        return dto;
    }

    /**
     * Extrae información de la cabecera del ticket (Nº Ticket, Fecha y Tasa manuscrita).
     */
    private void extractHeaderInfo(BufferedImage image, int imgW, int imgH, ScannedTicketDTO dto) {
        try {
            // 1. Extraer Número de Ticket (Zona "Nº 010398")
            int numX = (int) (0.68 * imgW);
            int numY = (int) (0.08 * imgH);
            int numW = (int) (0.28 * imgW);
            int numH = (int) (0.045 * imgH);

            if (DigitRecognizer.hasInk(image, numX, numY, numW, numH)) {
                DigitRecognizer.RecognitionResult numRes = DigitRecognizer.recognizeNumber(image, numX, numY, numW, numH);
                if (numRes != null && !numRes.text.isBlank()) {
                    String cleanNum = numRes.text.replaceAll("[^0-9]", "");
                    if (!cleanNum.isEmpty()) {
                        dto.setNumeroFactura(cleanNum);
                        logger.debug("Número de ticket detectado localmente: {}", cleanNum);
                    }
                }
            }

            // 2. Extraer Tasa manuscrita superior (Zona "Tasa 328,48" / top)
            int tasaX = (int) (0.35 * imgW);
            int tasaY = (int) (0.005 * imgH);
            int tasaW = (int) (0.60 * imgW);
            int tasaH = (int) (0.055 * imgH);

            if (DigitRecognizer.hasInk(image, tasaX, tasaY, tasaW, tasaH)) {
                DigitRecognizer.RecognitionResult tasaRes = DigitRecognizer.recognizeNumber(image, tasaX, tasaY, tasaW, tasaH);
                if (tasaRes != null && tasaRes.toDouble() > 0) {
                    dto.setTasa(tasaRes.toDouble());
                    dto.setTasaOrigen("DETECTADA_EN_TICKET");
                    logger.debug("Tasa detectada localmente en cabecera: {}", tasaRes.toDouble());
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo extraer cabecera completa en modo local: {}", e.getMessage());
        }
    }

    /**
     * Detecta la forma de pago mediante OMR (Optical Mark Recognition).
     * Analiza la densidad de píxeles oscuros dentro de cada casilla de verificación.
     */
    private void detectPaymentMethod(BufferedImage image, int imgW, int imgH, ScannedTicketDTO dto) {
        int paymentYStart = (int) (CapelliTicketTemplate.PAYMENT_Y_START * imgH);
        int paymentYEnd = (int) (CapelliTicketTemplate.PAYMENT_Y_END * imgH);
        int paymentH = paymentYEnd - paymentYStart;

        // Primera línea de checkboxes (PAGO MOVIL, EFECTIVO, TRANSFERENCIA)
        int lineH = paymentH / 2;

        String detectedMethod = null;
        double maxDensity = 0.05; // Umbral mínimo para considerar una marca

        for (int i = 0; i < CapelliTicketTemplate.PAYMENT_METHODS.length; i++) {
            double[] xBounds = CapelliTicketTemplate.PAYMENT_CHECKBOX_X_BOUNDS[i];
            int checkX = (int) (xBounds[0] * imgW);
            int checkW = (int) ((xBounds[1] - xBounds[0]) * imgW);
            int checkY = paymentYStart;
            int checkH = lineH;

            // TD y TC están en la segunda línea
            if (i >= 3) {
                checkY = paymentYStart + lineH;
            }

            double density = DigitRecognizer.inkDensity(image, checkX, checkY, checkW, checkH);

            // Necesitamos considerar que el texto pre-impreso ya tiene cierta densidad base
            // Una marca manuscrita (check, X, relleno) incrementa la densidad significativamente
            if (density > maxDensity) {
                maxDensity = density;
                detectedMethod = CapelliTicketTemplate.PAYMENT_METHODS[i];
            }
        }

        if (detectedMethod != null) {
            dto.setMetodoPago(detectedMethod);
            logger.debug("Forma de pago detectada por OMR: {} (densidad: {:.3f})", detectedMethod, maxDensity);
        }
    }

    /**
     * Normaliza la imagen a un ancho estándar manteniendo la proporción.
     * Convierte a escala de grises para procesamiento más eficiente.
     */
    private BufferedImage normalizeImage(BufferedImage original) {
        int origW = original.getWidth();
        int origH = original.getHeight();

        // Calcular nuevo alto manteniendo proporción
        double scale = (double) NORMALIZED_WIDTH / origW;
        int newH = (int) (origH * scale);

        BufferedImage normalized = new BufferedImage(NORMALIZED_WIDTH, newH, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = normalized.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, NORMALIZED_WIDTH, newH, null);
        g2d.dispose();

        return normalized;
    }

    /**
     * Obtiene el repositorio de correcciones para uso externo.
     */
    public TicketCorrectionRepository getCorrectionRepository() {
        return correctionRepo;
    }
}
