package app.service;

import app.exception.DatabaseException;
import app.model.AIKey;
import app.model.AIStatsDTO;
import app.model.ScannedTicketDTO;
import app.model.Servicio;
import app.model.Trabajadora;
import app.repository.AIKeyRepository;
import app.repository.AppSettingsRepository;
import app.repository.ServicioRepository;
import app.repository.ServicioRepositorySQLite;
import app.repository.TrabajadoraRepository;
import app.repository.TrabajadoraRepositorySQLite;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Servicio inteligente para el análisis y extracción de comandas/tickets de servicios
 * mediante la API de Google Gemini (Visión).
 * 
 * Incluye:
 * - Pool de múltiples API Keys con Failover (salto automático en error 429 / límite de cuota).
 * - Selector dinámico de modelos de Google.
 * - Registro en tiempo real de métricas, latencia y control de cuota diario.
 */
public class GeminiTicketScannerService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiTicketScannerService.class);

    public static final String SETTING_KEY_MODEL = "gemini_selected_model";
    public static final String SETTING_KEY_ENGINE_MODE = "ocr_engine_mode";
    public static final String ENGINE_MODE_GEMINI_AI = "GEMINI_AI";
    public static final String ENGINE_MODE_LOCAL_OCR = "LOCAL_OCR";
    public static final String DEFAULT_MODEL = "gemini-3.6-flash";

    private static final String[] FALLBACK_MODELS = new String[]{
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-2.5-pro",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b"
    };

    private final AppSettingsRepository appSettingsRepo;
    private final AIKeyRepository aiKeyRepo;
    private final ServicioRepository servicioRepo;
    private final TrabajadoraRepository trabajadoraRepo;
    private final HttpClient httpClient;

    public static class ConnectionTestResult {
        public final boolean success;
        public final String message;
        public final String modelUsed;

        public ConnectionTestResult(boolean success, String message, String modelUsed) {
            this.success = success;
            this.message = message;
            this.modelUsed = modelUsed;
        }
    }

    public GeminiTicketScannerService() {
        this.appSettingsRepo = new AppSettingsRepository();
        this.aiKeyRepo = new AIKeyRepository();
        this.servicioRepo = new ServicioRepositorySQLite();
        this.trabajadoraRepo = new TrabajadoraRepositorySQLite();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public AIKeyRepository getKeyRepository() {
        return aiKeyRepo;
    }

    /**
     * Limpia la clave de posibles espacios o comillas accidentales al copiar y pegar.
     */
    public static String sanitizeApiKey(String key) {
        if (key == null) return null;
        String clean = key.trim();
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }

    /**
     * Obtiene la primera API Key activa del pool o de variables de entorno.
     */
    public String getApiKey() {
        // 1. Variable de entorno
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return sanitizeApiKey(envKey);
        }

        // 2. Pool de API Keys en BD SQLite
        try {
            List<AIKey> activeKeys = aiKeyRepo.findActive();
            if (!activeKeys.isEmpty()) {
                return sanitizeApiKey(activeKeys.get(0).getApiKey());
            }
        } catch (DatabaseException e) {
            logger.warn("No se pudieron listar claves activas de SQLite", e);
        }

        // 3. Archivo .env local
        File envFile = new File(".env");
        if (envFile.exists()) {
            try (InputStream input = new FileInputStream(envFile)) {
                Properties prop = new Properties();
                prop.load(input);
                String fileKey = prop.getProperty("GEMINI_API_KEY");
                if (fileKey != null && !fileKey.isBlank()) {
                    return sanitizeApiKey(fileKey);
                }
            } catch (Exception e) {
                logger.debug("No se pudo leer .env", e);
            }
        }

        return null;
    }

    /**
     * Guarda una API Key como Principal (para compatibilidad).
     */
    public void saveApiKey(String apiKey) throws DatabaseException {
        String clean = sanitizeApiKey(apiKey);
        if (clean == null || clean.isBlank()) return;

        List<AIKey> all = aiKeyRepo.findAll();
        AIKey target = null;
        for (AIKey k : all) {
            if ("Cuenta Principal".equalsIgnoreCase(k.getLabel())) {
                target = k;
                break;
            }
        }

        if (target == null) {
            target = new AIKey("Cuenta Principal", clean);
        } else {
            target.setApiKey(clean);
            target.setActive(true);
        }
        aiKeyRepo.save(target);
        appSettingsRepo.setSetting("gemini_api_key", clean);
    }

    /**
     * Verifica si hay al menos una API Key activa configurada.
     */
    public boolean isApiKeyConfigured() {
        try {
            List<AIKey> active = aiKeyRepo.findActive();
            if (!active.isEmpty()) return true;
        } catch (Exception ignored) {}

        String env = getApiKey();
        return env != null && !env.isBlank();
    }

    /**
     * Obtiene el modelo preferido seleccionado por el usuario.
     */
    public String getSelectedModel() {
        try {
            String m = appSettingsRepo.getSetting(SETTING_KEY_MODEL);
            if (m != null && !m.isBlank()) {
                return m;
            }
        } catch (Exception ignored) {}
        return DEFAULT_MODEL;
    }

    /**
     * Guarda el modelo seleccionado.
     */
    public void saveSelectedModel(String model) throws DatabaseException {
        if (model == null || model.isBlank() || model.equalsIgnoreCase("Automático")) {
            appSettingsRepo.setSetting(SETTING_KEY_MODEL, "");
        } else {
            appSettingsRepo.setSetting(SETTING_KEY_MODEL, model.trim());
        }
    }

    /**
     * Obtiene el modo del motor de procesamiento configurado ("GEMINI_AI" o "LOCAL_OCR").
     */
    public String getEngineMode() {
        try {
            String mode = appSettingsRepo.getSetting(SETTING_KEY_ENGINE_MODE);
            if (mode != null && !mode.isBlank()) {
                return mode;
            }
        } catch (Exception ignored) {}
        // Por defecto, si hay API Key configurada usar GEMINI_AI, de lo contrario LOCAL_OCR
        return isApiKeyConfigured() ? ENGINE_MODE_GEMINI_AI : ENGINE_MODE_LOCAL_OCR;
    }

    /**
     * Guarda el modo del motor de procesamiento ("GEMINI_AI" o "LOCAL_OCR").
     */
    public void saveEngineMode(String mode) throws DatabaseException {
        if (mode == null || (!mode.equals(ENGINE_MODE_LOCAL_OCR) && !mode.equals(ENGINE_MODE_GEMINI_AI))) {
            mode = ENGINE_MODE_GEMINI_AI;
        }
        appSettingsRepo.setSetting(SETTING_KEY_ENGINE_MODE, mode);
        logger.info("Modo de motor de tickets actualizado a: {}", mode);
    }

    /**
     * Obtiene las estadísticas consolidadas y métricas de uso.
     */
    public AIStatsDTO getStats() {
        return aiKeyRepo.getUsageStats();
    }

    /**
     * Consulta dinámicamente a la API de Google los modelos disponibles y soportados para una clave.
     */
    public List<String> getAvailableModels(String apiKey) {
        List<String> models = new ArrayList<>();
        if (apiKey == null || apiKey.isBlank()) {
            models.addAll(List.of(FALLBACK_MODELS));
            return models;
        }

        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + encodedKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject root = new JSONObject(response.body());
                if (root.has("models")) {
                    JSONArray modelList = root.getJSONArray("models");
                    for (int i = 0; i < modelList.length(); i++) {
                        JSONObject m = modelList.getJSONObject(i);
                        String name = m.getString("name").replace("models/", "");
                        // Filtrar modelos no aptos para visión y generación de texto/json
                        if (name.contains("-tts") || name.contains("embedding") || name.contains("aqa") 
                                || name.contains("imagen") || name.contains("audio") || name.contains("whisper")
                                || name.contains("robotics") || name.contains("veo")) {
                            continue;
                        }

                        JSONArray methods = m.optJSONArray("supportedGenerationMethods");
                        boolean supportsGenerate = false;
                        if (methods != null) {
                            for (int j = 0; j < methods.length(); j++) {
                                if ("generateContent".equalsIgnoreCase(methods.getString(j))) {
                                    supportsGenerate = true;
                                    break;
                                }
                            }
                        }
                        if (supportsGenerate) {
                            models.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo listar modelos dinámicamente de Gemini: {}", e.getMessage());
        }

        if (models.isEmpty()) {
            models.addAll(List.of(FALLBACK_MODELS));
        } else {
            // Priorizar modelos más nuevos y veloces (3.6, 3.5, 2.5, flash)
            models.sort((a, b) -> {
                int scoreA = getModelScore(a);
                int scoreB = getModelScore(b);
                return Integer.compare(scoreB, scoreA);
            });
        }

        return models;
    }

    private int getModelScore(String name) {
        int score = 0;
        if (name.contains("3.6")) score += 120;
        if (name.contains("3.5")) score += 100;
        if (name.contains("2.5")) score += 80;
        if (name.contains("flash")) score += 50;
        if (name.contains("lite")) score += 20;
        if (name.contains("latest")) score += 10;
        if (name.contains("pro")) score += 5;
        return score;
    }

    /**
     * Prueba la conexión a la API de Gemini con una clave dada.
     */
    public ConnectionTestResult testConnectionDetailed(String rawApiKey) {
        String apiKey = sanitizeApiKey(rawApiKey);
        if (apiKey == null || apiKey.isBlank()) {
            return new ConnectionTestResult(false, "La clave está vacía.", null);
        }

        String lastErrorMsg = "Error desconocido";
        List<String> modelsToTry = getAvailableModels(apiKey);

        for (String model : modelsToTry) {
            try {
                String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + encodedKey;

                JSONObject textPart = new JSONObject();
                textPart.put("text", "Responde 'OK'");

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject content = new JSONObject();
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject root = new JSONObject();
                root.put("contents", contents);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    logger.info("Prueba exitosa con modelo Gemini: {}", model);
                    return new ConnectionTestResult(true, "¡Conexión exitosa! (" + model + ")", model);
                }

                String body = response.body();
                logger.warn("Respuesta de error de Gemini ({}): {}", response.statusCode(), body);
                try {
                    JSONObject errObj = new JSONObject(body);
                    if (errObj.has("error")) {
                        JSONObject errDetail = errObj.getJSONObject("error");
                        lastErrorMsg = errDetail.optString("message", "HTTP " + response.statusCode());
                    } else {
                        lastErrorMsg = "HTTP " + response.statusCode();
                    }
                } catch (Exception ex) {
                    lastErrorMsg = "HTTP " + response.statusCode();
                }

                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    break;
                }
                if (response.statusCode() == 404 || response.statusCode() == 400) {
                    continue;
                }
                break;
            } catch (Exception e) {
                logger.error("Excepción al probar conexión con Gemini ({})", model, e);
                lastErrorMsg = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "Error de red");
            }
        }

        return new ConnectionTestResult(false, lastErrorMsg, null);
    }

    public boolean testConnection(String apiKey) {
        return testConnectionDetailed(apiKey).success;
    }

    /**
     * Envía la imagen del ticket a Google Gemini con Failover multi-clave,
     * registro de latencia y control de cuota diario.
     */
    public ScannedTicketDTO scanTicketImage(byte[] imageBytes, String mimeType) throws Exception {
        List<AIKey> activeKeys = aiKeyRepo.findActive();
        if (activeKeys.isEmpty()) {
            String fallbackKey = getApiKey();
            if (fallbackKey == null || fallbackKey.isBlank()) {
                throw new IllegalStateException("No hay API Keys de Google Gemini configuradas. Por favor agrega una en los ajustes.");
            }
            activeKeys = List.of(new AIKey("Clave Sistema", fallbackKey));
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("La imagen proporcionada está vacía.");
        }

        // 1. Verificar si todas las claves alcanzaron el límite diario de 1500 por clave
        int totalRequestsToday = 0;
        int totalCapacity = activeKeys.size() * 1500;
        for (AIKey k : activeKeys) {
            totalRequestsToday += k.getRequestsToday();
        }

        if (totalRequestsToday >= totalCapacity && totalCapacity > 0) {
            throw new IllegalStateException(String.format(
                    "🛑 Se ha alcanzado el límite gratuito diario de todas las claves configuradas (%d / %d tickets).\n" +
                    "El cupo se renovará automáticamente mañana a las 00:00 UTC, o puedes agregar otra API Key en los ajustes.",
                    totalRequestsToday, totalCapacity
            ));
        }

        // 2. Obtener catálogo contextual de BD
        StringBuilder catalogoServicios = new StringBuilder();
        try {
            List<Servicio> servicios = servicioRepo.findAll();
            for (Servicio s : servicios) {
                catalogoServicios.append("- ").append(s.getNombre())
                        .append(" (Corto: $").append(s.getPrecioCorto())
                        .append(", Mediano: $").append(s.getPrecioMediano())
                        .append(", Largo: $").append(s.getPrecioLargo()).append(")\n");
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar el catálogo de servicios para el prompt", e);
        }

        StringBuilder catalogoTrabajadoras = new StringBuilder();
        try {
            List<Trabajadora> trabajadoras = trabajadoraRepo.findAll();
            for (Trabajadora t : trabajadoras) {
                catalogoTrabajadoras.append("- ").append(t.getNombreCompleto())
                        .append(" (Cédula: ").append(t.getCedula()).append(")\n");
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar la lista de trabajadoras para el prompt", e);
        }

        // 3. Construir Prompt del Sistema
        String prompt = """
                Eres un asistente de facturación experto para un salón de belleza llamado 'Salón Capelli'.
                Tu tarea es analizar la foto adjunta de un ticket o comanda de servicios (impreso o manuscrito) y extraer con la mayor precisión posible los datos para facturación.
                
                CATÁLOGO DE SERVICIOS REGISTRADOS EN EL SISTEMA:
                %s
                
                TRABAJADORAS REGISTRADAS EN EL SISTEMA:
                %s
                
                INSTRUCCIONES IMPORTANTES:
                1. Extrae el número de ticket / comanda o factura si está visible (ej. "010398" de "Nº 010398").
                2. Extrae la fecha escrita en el ticket (ej. "12-04-24", "12/04/2024").
                3. Extrae la tasa de cambio si está escrita en la parte superior o encabezado (ej. 328.48 de "Tasa 328,48" o "Tasa: 328.48"). Si no está escrita, coloca 0.0.
                4. Identifica el nombre o cédula del cliente si está escrito en la sección NOMBRE / C.I. Si no aparece, deja cliente_nombre y cliente_cedula en null.
                5. Identifica la trabajadora / estilista general o por cada fila que atendió. Asóciala con el nombre más parecido de la lista de trabajadoras de la base de datos (ej. "Marvi" -> "Maria Virginia", "Jey" -> "Jeimy").
                6. Identifica cada servicio o producto realizado en las filas. Para cada servicio, busca el nombre exacto más coincidente de la lista del catálogo. Si en el ticket se especifica longitud de cabello (Corto, Mediano, Largo, Extensiones), regístralo en 'tipo_cabello'.
                7. Si hay fila de Propina (generalmente fila 22 o concepto 'Propina'), márcala con "es_propina": true.
                8. Extrae los precios o montos de cada servicio. Si el precio no está escrito, usa 0.0.
                9. Identifica el método de pago si está marcado o escrito (EFECTIVO, PAGO_MOVIL, PUNTO, ZELLE, TRANSFERENCIA, DIVISAS, etc.) y su número de referencia si existe (ej. "CtlQMcZV53alt").
                10. Calcula o extrae el total general.
                
                Debes responder EXCLUSIVAMENTE un objeto JSON válido con este esquema:
                {
                  "numero_factura": "String o null",
                  "fecha": "String (DD-MM-YYYY o DD-MM-YY) o null",
                  "tasa": 0.00,
                  "cliente_nombre": "String o null",
                  "cliente_cedula": "String o null",
                  "cliente_telefono": "String o null",
                  "trabajadora_nombre": "String o null",
                  "items": [
                    {
                      "descripcion": "Nombre exacto o más cercano del servicio",
                      "precio": 0.00,
                      "tipo_cabello": "CORTO | MEDIANO | LARGO | EXTENSIONES | null",
                      "es_producto": false,
                      "es_propina": false,
                      "trabajadora_nombre": "Nombre de la colaboradora de esta fila o null"
                    }
                  ],
                  "metodo_pago": "EFECTIVO | PAGO_MOVIL | PUNTO | ZELLE | TRANSFERENCIA | DIVISAS | null",
                  "referencia_pago": "String o null",
                  "total": 0.00,
                  "notas": "Detalles adicionales observados en el ticket o null"
                }
                """.formatted(catalogoServicios.toString(), catalogoTrabajadoras.toString());

        // 4. Preparar Payload JSON
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg");
        inlineData.put("data", base64Image);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", inlineData);

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(textPart);
        parts.put(imagePart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.1);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);
        requestBody.put("generationConfig", generationConfig);

        String userSelectedModel = getSelectedModel();

        // 5. Iterar sobre las claves activas (FAILOVER POOL)
        List<String> errorLogs = new ArrayList<>();

        for (int kIndex = 0; kIndex < activeKeys.size(); kIndex++) {
            AIKey currentKey = activeKeys.get(kIndex);
            String apiKey = sanitizeApiKey(currentKey.getApiKey());
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            List<String> modelsToTry = new ArrayList<>();
            if (userSelectedModel != null && !userSelectedModel.isBlank() && !userSelectedModel.equalsIgnoreCase("Automático")) {
                modelsToTry.add(userSelectedModel);
            }
            for (String m : getAvailableModels(apiKey)) {
                if (!modelsToTry.contains(m)) {
                    modelsToTry.add(m);
                }
            }

            for (String model : modelsToTry) {
                long startTime = System.currentTimeMillis();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + encodedKey;

                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(45))
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                            .build();

                    logger.info("Enviando ticket a Gemini (Clave: '{}', Modelo: '{}')...", currentKey.getLabel(), model);
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    long durationMs = System.currentTimeMillis() - startTime;

                    if (response.statusCode() == 200) {
                        ScannedTicketDTO dto = parseGeminiResponse(response.body());
                        aiKeyRepo.recordUsage(currentKey.getId(), currentKey.getLabel(), model, durationMs, "SUCCESS", null);
                        logger.info("Ticket procesado con éxito en {} ms usando clave '{}'", durationMs, currentKey.getLabel());

                        // Post-procesamiento de Tasa (Fallback Euro Oficial por Fecha de dolarapi)
                        enrichTasaIfMissing(dto);

                        return dto;
                    }

                    // Extraer detalle de error
                    String errorMsg = "HTTP " + response.statusCode();
                    try {
                        JSONObject errObj = new JSONObject(response.body());
                        if (errObj.has("error")) {
                            errorMsg = errObj.getJSONObject("error").optString("message", errorMsg);
                        }
                    } catch (Exception ignored) {}

                    // Manejo de Rate Limit (429) o Cuota Agotada
                    if (response.statusCode() == 429) {
                        logger.warn("Clave '{}' alcanzó límite de tasa/cuota (429): {}. Saltando a siguiente clave...", currentKey.getLabel(), errorMsg);
                        aiKeyRepo.recordUsage(currentKey.getId(), currentKey.getLabel(), model, durationMs, "RATE_LIMIT_429", errorMsg);
                        errorLogs.add(currentKey.getLabel() + ": Límite 429 (" + errorMsg + ")");
                        // Salir del bucle de modelos para esta clave y pasar a la siguiente clave del pool
                        break;
                    }

                    if (response.statusCode() == 404 || response.statusCode() == 400) {
                        // Intentar siguiente modelo de la lista para esta misma clave
                        continue;
                    }

                    // Otro error de API
                    logger.warn("Error en Gemini con clave '{}' ({}): {}", currentKey.getLabel(), response.statusCode(), errorMsg);
                    aiKeyRepo.recordUsage(currentKey.getId(), currentKey.getLabel(), model, durationMs, "ERROR", errorMsg);
                    errorLogs.add(currentKey.getLabel() + " (" + model + "): " + errorMsg);
                    break;

                } catch (Exception ex) {
                    long durationMs = System.currentTimeMillis() - startTime;
                    logger.error("Fallo al conectar con Gemini (Clave: '{}', Modelo: '{}'): {}", currentKey.getLabel(), model, ex.getMessage());
                    aiKeyRepo.recordUsage(currentKey.getId(), currentKey.getLabel(), model, durationMs, "ERROR", ex.getMessage());
                    errorLogs.add(currentKey.getLabel() + ": " + ex.getMessage());
                    break;
                }
            }
        }

        throw new RuntimeException("No se pudo procesar la imagen con ninguna de las API Keys disponibles.\nDetalles: " + String.join(" | ", errorLogs));
    }

    /**
     * Completa la tasa si no fue identificada en el ticket mediante el endpoint de Euro Oficial por Fecha.
     */
    public static void enrichTasaIfMissing(ScannedTicketDTO dto) {
        if (dto == null) return;

        if (dto.getTasa() > 0) {
            dto.setTasaOrigen("DETECTADA_EN_TICKET");
            return;
        }

        if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
            double euroRate = BCVService.getEuroOficialRate(dto.getFecha());
            if (euroRate > 0) {
                dto.setTasa(euroRate);
                dto.setTasaOrigen("EURO_HISTORICO_DOLARAPI");
                logger.info("Tasa no encontrada en ticket. Asignada automáticamente vía Euro Oficial Histórico: {}", euroRate);
                return;
            }
        }

        double bcvRate = BCVService.getBCVRateSafe();
        dto.setTasa(bcvRate);
        dto.setTasaOrigen("BCV_ACTUAL");
    }

    /**
     * Parsea la respuesta JSON de Gemini y la convierte a ScannedTicketDTO.
     */
    private ScannedTicketDTO parseGeminiResponse(String responseJson) {
        JSONObject root = new JSONObject(responseJson);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini no devolvió ningún candidato de respuesta.");
        }

        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.optJSONObject("content");
        if (content == null) {
            throw new RuntimeException("Respuesta vacía de Gemini.");
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException("No hay partes de texto en la respuesta de Gemini.");
        }

        String rawText = parts.getJSONObject(0).optString("text", "{}");
        logger.debug("Texto JSON devuelto por Gemini: {}", rawText);

        JSONObject data = new JSONObject(rawText);
        ScannedTicketDTO dto = new ScannedTicketDTO();

        dto.setNumeroFactura(optStringOrNull(data, "numero_factura"));
        dto.setFecha(optStringOrNull(data, "fecha"));
        dto.setTasa(data.optDouble("tasa", 0.0));

        dto.setClienteNombre(optStringOrNull(data, "cliente_nombre"));
        dto.setClienteCedula(optStringOrNull(data, "cliente_cedula"));
        dto.setClienteTelefono(optStringOrNull(data, "cliente_telefono"));
        dto.setTrabajadoraNombre(optStringOrNull(data, "trabajadora_nombre"));
        dto.setMetodoPago(optStringOrNull(data, "metodo_pago"));
        dto.setReferenciaPago(optStringOrNull(data, "referencia_pago"));
        dto.setTotalDetectado(data.optDouble("total", 0.0));
        dto.setNotas(optStringOrNull(data, "notas"));

        JSONArray itemsArr = data.optJSONArray("items");
        if (itemsArr != null) {
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.getJSONObject(i);
                String desc = itemObj.optString("descripcion", "Servicio sin nombre");
                double precio = itemObj.optDouble("precio", 0.0);
                String tipoCabello = optStringOrNull(itemObj, "tipo_cabello");
                boolean esProd = itemObj.optBoolean("es_producto", false);
                boolean esPropina = itemObj.optBoolean("es_propina", false);
                String trabFila = optStringOrNull(itemObj, "trabajadora_nombre");

                ScannedTicketDTO.ScannedItemDTO item = new ScannedTicketDTO.ScannedItemDTO(desc, precio, tipoCabello, esProd);
                item.setEsPropina(esPropina);
                item.setTrabajadoraNombre(trabFila);
                item.setConfidence(0.90);
                item.setRowNumber(i + 1);

                dto.addItem(item);
            }
        }

        return dto;
    }

    private String optStringOrNull(JSONObject obj, String key) {
        if (!obj.has(key) || obj.isNull(key)) {
            return null;
        }
        String val = obj.optString(key, "").trim();
        if (val.equalsIgnoreCase("null") || val.isEmpty()) {
            return null;
        }
        return val;
    }
}
