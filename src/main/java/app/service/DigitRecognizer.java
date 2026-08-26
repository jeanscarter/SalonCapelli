package app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor ligero de reconocimiento de dígitos manuscritos.
 * 
 * Funciona sin dependencias externas (sin Tesseract, sin OpenCV).
 * Utiliza un enfoque de "feature zones" donde cada carácter se divide
 * en zonas y se analiza la densidad de píxeles oscuros para clasificarlo.
 * 
 * Precisión optimizada para números de montos en tickets (0-9, punto, coma).
 */
public class DigitRecognizer {

    private static final Logger logger = LoggerFactory.getLogger(DigitRecognizer.class);

    /** Umbral de binarización (0-255). Píxeles más oscuros = tinta */
    private static final int BINARIZE_THRESHOLD = 140;
    
    /** Mínimo porcentaje de píxeles oscuros para considerar que hay tinta */
    private static final double MIN_INK_DENSITY = 0.03;

    /**
     * Detecta si una región de imagen contiene tinta (escritura manuscrita).
     * 
     * @param image  Imagen completa del ticket
     * @param x      Coordenada X de inicio de la región
     * @param y      Coordenada Y de inicio de la región
     * @param width  Ancho de la región
     * @param height Alto de la región
     * @return true si la región contiene suficientes píxeles oscuros
     */
    public static boolean hasInk(BufferedImage image, int x, int y, int width, int height) {
        if (image == null || width <= 0 || height <= 0) return false;

        // Clamp coordinates
        int x2 = Math.min(x + width, image.getWidth());
        int y2 = Math.min(y + height, image.getHeight());
        x = Math.max(0, x);
        y = Math.max(0, y);

        int darkPixels = 0;
        int totalPixels = 0;

        for (int py = y; py < y2; py++) {
            for (int px = x; px < x2; px++) {
                totalPixels++;
                int rgb = image.getRGB(px, py);
                int gray = getGrayscale(rgb);
                if (gray < BINARIZE_THRESHOLD) {
                    darkPixels++;
                }
            }
        }

        if (totalPixels == 0) return false;
        double density = (double) darkPixels / totalPixels;
        return density > MIN_INK_DENSITY;
    }

    /**
     * Calcula la densidad de tinta en una región (0.0 a 1.0).
     */
    public static double inkDensity(BufferedImage image, int x, int y, int width, int height) {
        if (image == null || width <= 0 || height <= 0) return 0.0;

        int x2 = Math.min(x + width, image.getWidth());
        int y2 = Math.min(y + height, image.getHeight());
        x = Math.max(0, x);
        y = Math.max(0, y);

        int darkPixels = 0;
        int totalPixels = 0;

        for (int py = y; py < y2; py++) {
            for (int px = x; px < x2; px++) {
                totalPixels++;
                int gray = getGrayscale(image.getRGB(px, py));
                if (gray < BINARIZE_THRESHOLD) {
                    darkPixels++;
                }
            }
        }

        return totalPixels > 0 ? (double) darkPixels / totalPixels : 0.0;
    }

    /**
     * Extrae y reconoce un número de una región de imagen.
     * 
     * Pipeline:
     * 1. Binariza la región
     * 2. Encuentra los límites reales del contenido (trim whitespace)
     * 3. Segmenta en caracteres individuales por proyección vertical
     * 4. Clasifica cada carácter usando features de zona
     * 
     * @param image  Imagen completa del ticket
     * @param x      Coordenada X de inicio de la celda de monto
     * @param y      Coordenada Y de inicio de la celda de monto
     * @param width  Ancho de la celda de monto
     * @param height Alto de la celda de monto
     * @return RecognitionResult con el número reconocido y la confianza
     */
    public static RecognitionResult recognizeNumber(BufferedImage image, int x, int y, int width, int height) {
        if (image == null || width <= 0 || height <= 0) {
            return new RecognitionResult("", 0.0);
        }

        try {
            // 1. Extraer y binarizar la subimagen
            int x2 = Math.min(x + width, image.getWidth());
            int y2 = Math.min(y + height, image.getHeight());
            x = Math.max(0, x);
            y = Math.max(0, y);
            
            int actualW = x2 - x;
            int actualH = y2 - y;
            if (actualW <= 2 || actualH <= 2) return new RecognitionResult("", 0.0);

            boolean[][] binary = new boolean[actualH][actualW];
            for (int py = 0; py < actualH; py++) {
                for (int px = 0; px < actualW; px++) {
                    int gray = getGrayscale(image.getRGB(x + px, y + py));
                    binary[py][px] = gray < BINARIZE_THRESHOLD;
                }
            }

            // 2. Trim whitespace (encontrar bounding box del contenido real)
            int minX = actualW, maxX = 0, minY = actualH, maxY = 0;
            for (int py = 0; py < actualH; py++) {
                for (int px = 0; px < actualW; px++) {
                    if (binary[py][px]) {
                        minX = Math.min(minX, px);
                        maxX = Math.max(maxX, px);
                        minY = Math.min(minY, py);
                        maxY = Math.max(maxY, py);
                    }
                }
            }

            if (minX >= maxX || minY >= maxY) {
                return new RecognitionResult("", 0.0);
            }

            // 3. Segmentar en caracteres por proyección vertical
            List<int[]> charBounds = segmentCharacters(binary, minX, maxX, minY, maxY);

            if (charBounds.isEmpty()) {
                return new RecognitionResult("", 0.0);
            }

            // 4. Clasificar cada segmento
            StringBuilder result = new StringBuilder();
            double totalConfidence = 0;
            int charCount = 0;

            for (int[] bounds : charBounds) {
                int cx = bounds[0], cw = bounds[1] - bounds[0];
                if (cw < 2) continue;

                CharResult cr = classifyCharacter(binary, cx, bounds[1], minY, maxY);
                result.append(cr.character);
                totalConfidence += cr.confidence;
                charCount++;
            }

            double avgConf = charCount > 0 ? totalConfidence / charCount : 0.0;
            String numStr = result.toString().trim();

            // Validar que parece un número
            numStr = sanitizeNumber(numStr);

            return new RecognitionResult(numStr, avgConf);

        } catch (Exception e) {
            logger.debug("Error en reconocimiento de dígitos: {}", e.getMessage());
            return new RecognitionResult("", 0.0);
        }
    }

    /**
     * Segmenta caracteres por gaps en la proyección vertical.
     */
    private static List<int[]> segmentCharacters(boolean[][] binary, int minX, int maxX, int minY, int maxY) {
        List<int[]> segments = new ArrayList<>();
        int contentH = maxY - minY + 1;

        // Calcular proyección vertical (contar píxeles oscuros por columna)
        int[] projection = new int[maxX - minX + 1];
        for (int px = minX; px <= maxX; px++) {
            int count = 0;
            for (int py = minY; py <= maxY; py++) {
                if (py < binary.length && px < binary[py].length && binary[py][px]) {
                    count++;
                }
            }
            projection[px - minX] = count;
        }

        // Encontrar segmentos continuos de tinta (con gap mínimo)
        int minGap = Math.max(2, contentH / 8);
        boolean inChar = false;
        int charStart = 0;
        int gapCount = 0;

        for (int i = 0; i < projection.length; i++) {
            if (projection[i] > 0) {
                if (!inChar) {
                    charStart = minX + i;
                    inChar = true;
                }
                gapCount = 0;
            } else {
                if (inChar) {
                    gapCount++;
                    if (gapCount >= minGap) {
                        segments.add(new int[]{charStart, minX + i - gapCount + 1});
                        inChar = false;
                    }
                }
            }
        }

        if (inChar) {
            segments.add(new int[]{charStart, maxX + 1});
        }

        return segments;
    }

    /**
     * Clasifica un carácter individual usando análisis de zonas (zone features).
     * 
     * Divide el glifo en una cuadrícula 3x3 y analiza la densidad en cada zona
     * para discriminar entre dígitos. Adicionalmente usa aspect ratio y 
     * distribución horizontal/vertical.
     */
    private static CharResult classifyCharacter(boolean[][] binary, int xStart, int xEnd, int yStart, int yEnd) {
        int w = xEnd - xStart;
        int h = yEnd - yStart;

        if (w <= 0 || h <= 0) return new CharResult('?', 0.0);

        double aspectRatio = (double) w / h;

        // Si es muy estrecho, podría ser un 1, punto o coma
        if (aspectRatio < 0.25) {
            // Verificar si está en la mitad inferior (punto/coma) o es alto (1)
            double bottomDensity = zoneDensity(binary, xStart, xEnd, yStart + h / 2, yEnd);
            double topDensity = zoneDensity(binary, xStart, xEnd, yStart, yStart + h / 2);
            
            if (h < w * 3 && bottomDensity > 0.3 && topDensity < 0.1) {
                return new CharResult('.', 0.70);
            }
            return new CharResult('1', 0.65);
        }

        // Dividir en cuadrícula 3x3
        int zoneW = w / 3;
        int zoneH = h / 3;
        double[][] zones = new double[3][3];

        for (int zy = 0; zy < 3; zy++) {
            for (int zx = 0; zx < 3; zx++) {
                int zx1 = xStart + zx * zoneW;
                int zx2 = (zx == 2) ? xEnd : zx1 + zoneW;
                int zy1 = yStart + zy * zoneH;
                int zy2 = (zy == 2) ? yEnd : zy1 + zoneH;
                zones[zy][zx] = zoneDensity(binary, zx1, zx2, zy1, zy2);
            }
        }

        // Centro horizontal (para detectar huecos centrales en 0, 8, etc.)
        double centerDensity = zones[1][1];
        double topCenter = zones[0][1];
        double bottomCenter = zones[2][1];
        double leftMiddle = zones[1][0];
        double rightMiddle = zones[1][2];
        double topLeft = zones[0][0];
        double topRight = zones[0][2];
        double bottomLeft = zones[2][0];
        double bottomRight = zones[2][2];

        // Total density
        double totalDensity = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                totalDensity += zones[i][j];
        totalDensity /= 9.0;

        // Heurísticas de clasificación
        return classifyByZones(zones, centerDensity, topCenter, bottomCenter,
                leftMiddle, rightMiddle, topLeft, topRight, bottomLeft, bottomRight,
                totalDensity, aspectRatio);
    }

    private static CharResult classifyByZones(double[][] z, double center, double topC, double botC,
                                               double leftM, double rightM, double tl, double tr,
                                               double bl, double br, double total, double aspect) {
        // 0: Hueco central, bordes altos
        if (center < 0.2 && tl > 0.3 && tr > 0.3 && bl > 0.3 && br > 0.3) {
            return new CharResult('0', 0.70);
        }

        // 1: Muy estrecho, ya manejado arriba, o concentrado a la derecha
        if (aspect < 0.4 && rightM > leftM * 1.5) {
            return new CharResult('1', 0.60);
        }

        // 2: Arriba derecha, abajo izquierda, diagonal
        if (tr > 0.3 && bl > 0.3 && tl < 0.2 && br < 0.15) {
            return new CharResult('2', 0.55);
        }
        if (topC > 0.3 && botC > 0.3 && rightM > leftM && bl < 0.15) {
            return new CharResult('2', 0.50);
        }

        // 3: Concentrado a la derecha
        if (tr > 0.3 && rightM > 0.3 && br > 0.3 && leftM < 0.15 && tl < 0.15) {
            return new CharResult('3', 0.55);
        }

        // 4: Top-left + center horizontal + right vertical
        if (tl > 0.3 && leftM > 0.2 && center > 0.3 && br > 0.3 && bl < 0.15) {
            return new CharResult('4', 0.55);
        }

        // 5: Top-left, center, bottom-right (opuesto a 2)
        if (tl > 0.3 && center > 0.2 && br > 0.3 && tr < 0.15 && bl < 0.2) {
            return new CharResult('5', 0.55);
        }
        if (topC > 0.2 && leftM > 0.3 && botC > 0.3 && bl > 0.3) {
            return new CharResult('5', 0.50);
        }

        // 6: Similar a 0 pero más pesado arriba-izquierda
        if (leftM > 0.35 && bl > 0.3 && botC > 0.3 && br > 0.3 && tr < 0.2) {
            return new CharResult('6', 0.55);
        }

        // 7: Solo arriba y diagonal derecha
        if (topC > 0.35 && tr > 0.3 && tl > 0.2 && bl < 0.15 && leftM < 0.15 && br > 0.15) {
            return new CharResult('7', 0.55);
        }

        // 8: Todo denso, como dos 0 apilados
        if (total > 0.3 && center > 0.25 && topC > 0.25 && botC > 0.25) {
            return new CharResult('8', 0.55);
        }

        // 9: Opuesto a 6 — pesado arriba, ligero abajo-izquierda
        if (tl > 0.3 && topC > 0.3 && tr > 0.3 && rightM > 0.3 && bl < 0.2) {
            return new CharResult('9', 0.55);
        }

        // Fallback: usar la heurística más simple — encontrar el dígito con el patrón más cercano
        return bestGuessDigit(z, total, aspect);
    }

    private static CharResult bestGuessDigit(double[][] z, double total, double aspect) {
        // Simplificación: dígito más probable basado en distribución general
        if (total > 0.35) return new CharResult('8', 0.35);
        if (total > 0.25) return new CharResult('0', 0.35);
        if (total < 0.12) return new CharResult('1', 0.35);
        return new CharResult('5', 0.25); // fallback genérico
    }

    private static double zoneDensity(boolean[][] binary, int x1, int x2, int y1, int y2) {
        int dark = 0, total = 0;
        for (int y = Math.max(0, y1); y < Math.min(binary.length, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(binary[y].length, x2); x++) {
                total++;
                if (binary[y][x]) dark++;
            }
        }
        return total > 0 ? (double) dark / total : 0.0;
    }

    /**
     * Limpia y valida una cadena para que parezca un número válido.
     */
    private static String sanitizeNumber(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        boolean hasDecimal = false;
        
        for (char c : raw.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if ((c == '.' || c == ',') && !hasDecimal) {
                sb.append('.');
                hasDecimal = true;
            }
        }
        
        String result = sb.toString();
        
        // Eliminar puntos iniciales o finales
        if (result.startsWith(".")) result = result.substring(1);
        if (result.endsWith(".")) result = result.substring(0, result.length() - 1);
        
        return result;
    }

    private static int getGrayscale(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    /**
     * Resultado del reconocimiento de un número completo.
     */
    public static class RecognitionResult {
        public final String text;
        public final double confidence;

        public RecognitionResult(String text, double confidence) {
            this.text = text;
            this.confidence = confidence;
        }

        public double toDouble() {
            try {
                return text.isEmpty() ? 0.0 : Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        @Override
        public String toString() {
            return String.format("'%s' (%.0f%%)", text, confidence * 100);
        }
    }

    /**
     * Resultado de clasificación de un carácter individual.
     */
    private static class CharResult {
        final char character;
        final double confidence;

        CharResult(char character, double confidence) {
            this.character = character;
            this.confidence = confidence;
        }
    }
}
