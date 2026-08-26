package app.service;

import java.util.List;
import java.util.Map;

/**
 * Utilidad de coincidencia difusa (Fuzzy Matching) para textos manuscritos.
 * 
 * Utiliza Distancia de Levenshtein normalizada combinada con:
 * - Diccionario de apodos conocidos del salón
 * - Correcciones aprendidas de la tabla ticket_corrections
 * - Coincidencia por prefijos e iniciales
 */
public final class FuzzyMatcher {

    private FuzzyMatcher() {}

    /**
     * Calcula la distancia de Levenshtein entre dos cadenas.
     * @return Número de operaciones (inserción, eliminación, sustitución) necesarias
     */
    public static int levenshteinDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        
        int lenA = a.length();
        int lenB = b.length();
        
        if (lenA == 0) return lenB;
        if (lenB == 0) return lenA;
        
        int[][] dp = new int[lenA + 1][lenB + 1];
        
        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;
        
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (Character.toLowerCase(a.charAt(i - 1)) == Character.toLowerCase(b.charAt(j - 1))) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[lenA][lenB];
    }

    /**
     * Calcula la similitud normalizada entre dos cadenas (0.0 a 1.0).
     * 1.0 = idénticas, 0.0 = totalmente diferentes.
     */
    public static double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        
        int dist = levenshteinDistance(a, b);
        return 1.0 - ((double) dist / maxLen);
    }

    /**
     * Busca la mejor coincidencia para un texto raw entre una lista de candidatos.
     * 
     * @param rawText      Texto leído del ticket (ej: "Marvi", "Sey")
     * @param candidates   Lista de nombres válidos del sistema
     * @param minThreshold Similitud mínima para considerar una coincidencia (ej: 0.45)
     * @return MatchResult con el mejor candidato y score, o null si ninguno supera el umbral
     */
    public static MatchResult findBestMatch(String rawText, List<String> candidates, double minThreshold) {
        if (rawText == null || rawText.isBlank() || candidates == null || candidates.isEmpty()) {
            return null;
        }

        String normalizedRaw = normalize(rawText);

        // 1. Primero revisar apodos conocidos del salón
        String nicknameMatch = resolveNickname(normalizedRaw);
        if (nicknameMatch != null) {
            for (String candidate : candidates) {
                if (normalize(candidate).contains(normalize(nicknameMatch)) ||
                    normalize(nicknameMatch).contains(normalize(candidate))) {
                    return new MatchResult(candidate, 0.95, "apodo conocido");
                }
            }
        }

        // 2. Coincidencia exacta (case-insensitive)
        for (String candidate : candidates) {
            if (normalize(candidate).equals(normalizedRaw)) {
                return new MatchResult(candidate, 1.0, "exacta");
            }
        }

        // 3. Coincidencia por contención
        for (String candidate : candidates) {
            String normCandidate = normalize(candidate);
            if (normCandidate.contains(normalizedRaw) || normalizedRaw.contains(normCandidate)) {
                double sim = similarity(normalizedRaw, normCandidate);
                return new MatchResult(candidate, Math.max(sim, 0.80), "contención");
            }
        }

        // 4. Coincidencia por primer nombre (frecuente en tickets manuscritos)
        for (String candidate : candidates) {
            String firstName = normalize(candidate).split("\\s+")[0];
            if (firstName.equals(normalizedRaw) || similarity(firstName, normalizedRaw) > 0.75) {
                return new MatchResult(candidate, similarity(firstName, normalizedRaw), "primer nombre");
            }
        }

        // 5. Levenshtein general
        MatchResult best = null;
        for (String candidate : candidates) {
            double sim = similarity(normalizedRaw, normalize(candidate));
            
            // También comparar contra cada palabra individual del candidato
            String[] parts = normalize(candidate).split("\\s+");
            for (String part : parts) {
                double partSim = similarity(normalizedRaw, part);
                sim = Math.max(sim, partSim);
            }
            
            if (sim >= minThreshold && (best == null || sim > best.confidence)) {
                best = new MatchResult(candidate, sim, "levenshtein");
            }
        }

        return best;
    }

    /**
     * Resuelve un apodo conocido del salón a su nombre real.
     */
    public static String resolveNickname(String rawText) {
        if (rawText == null) return null;
        String lower = rawText.toLowerCase().trim();
        
        // Buscar en el diccionario de apodos del template
        String nickname = CapelliTicketTemplate.KNOWN_NICKNAMES.get(lower);
        if (nickname != null) return nickname;

        // Buscar coincidencias parciales
        for (Map.Entry<String, String> entry : CapelliTicketTemplate.KNOWN_NICKNAMES.entrySet()) {
            if (lower.startsWith(entry.getKey()) || entry.getKey().startsWith(lower)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Normaliza un texto para comparación: lowercase, trim, elimina acentos comunes.
     */
    public static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim()
                .replace("á", "a").replace("é", "e")
                .replace("í", "i").replace("ó", "o")
                .replace("ú", "u").replace("ñ", "n")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * Resultado de una coincidencia difusa.
     */
    public static class MatchResult {
        public final String matchedValue;
        public final double confidence; // 0.0 a 1.0
        public final String method;    // Método que generó la coincidencia

        public MatchResult(String matchedValue, double confidence, String method) {
            this.matchedValue = matchedValue;
            this.confidence = Math.min(1.0, Math.max(0.0, confidence));
            this.method = method;
        }

        @Override
        public String toString() {
            return String.format("'%s' (%.0f%% via %s)", matchedValue, confidence * 100, method);
        }
    }
}
