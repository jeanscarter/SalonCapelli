package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para el sistema de aprendizaje local (ticket_corrections).
 * 
 * Almacena las correcciones que el usuario realiza sobre los datos prellenados
 * del ticket, permitiendo que el sistema mejore con el uso.
 */
public class TicketCorrectionRepository {

    private static final Logger logger = LoggerFactory.getLogger(TicketCorrectionRepository.class);

    /**
     * Guarda o incrementa la frecuencia de una corrección.
     * Si ya existe el par (fieldType, rawValue, correctedValue), incrementa frequency.
     */
    public void saveCorrection(String fieldType, String rawValue, String correctedValue, int rowNumber) {
        if (rawValue == null || correctedValue == null) return;
        if (rawValue.equalsIgnoreCase(correctedValue)) return; // No guardar si no hubo cambio

        String sqlCheck = "SELECT id, frequency FROM ticket_corrections WHERE field_type = ? AND LOWER(raw_value) = LOWER(?) AND LOWER(corrected_value) = LOWER(?)";
        String sqlUpdate = "UPDATE ticket_corrections SET frequency = frequency + 1 WHERE id = ?";
        String sqlInsert = "INSERT INTO ticket_corrections (field_type, raw_value, corrected_value, row_number) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect()) {
            // Verificar si ya existe
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
                pstmt.setString(1, fieldType);
                pstmt.setString(2, rawValue.trim());
                pstmt.setString(3, correctedValue.trim());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt("id");
                    try (PreparedStatement upd = conn.prepareStatement(sqlUpdate)) {
                        upd.setInt(1, id);
                        upd.executeUpdate();
                    }
                    logger.debug("Corrección incrementada: {} '{}' → '{}' (freq++)", fieldType, rawValue, correctedValue);
                    return;
                }
            }

            // Insertar nueva
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setString(1, fieldType);
                pstmt.setString(2, rawValue.trim());
                pstmt.setString(3, correctedValue.trim());
                pstmt.setInt(4, rowNumber);
                pstmt.executeUpdate();
            }
            logger.info("Nueva corrección aprendida: {} '{}' → '{}'", fieldType, rawValue, correctedValue);

        } catch (DatabaseException | SQLException e) {
            logger.error("Error al guardar corrección de ticket", e);
        }
    }

    /**
     * Busca la corrección más frecuente para un valor raw dado.
     * 
     * @param fieldType Tipo de campo (COLABORADOR, MONTO, SERVICIO, CLIENTE)
     * @param rawValue  Valor leído originalmente
     * @return Valor corregido más frecuente, o null si no hay correcciones previas
     */
    public String findBestCorrection(String fieldType, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;

        String sql = "SELECT corrected_value, frequency FROM ticket_corrections " +
                     "WHERE field_type = ? AND LOWER(raw_value) = LOWER(?) " +
                     "ORDER BY frequency DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fieldType);
            pstmt.setString(2, rawValue.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String corrected = rs.getString("corrected_value");
                int freq = rs.getInt("frequency");
                logger.debug("Corrección encontrada: '{}' → '{}' (frecuencia: {})", rawValue, corrected, freq);
                return corrected;
            }
        } catch (DatabaseException | SQLException e) {
            logger.debug("Error al buscar corrección: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Obtiene las correcciones más frecuentes para un tipo de campo.
     * Útil para mostrar sugerencias en la UI.
     */
    public List<CorrectionEntry> getTopCorrections(String fieldType, int limit) {
        List<CorrectionEntry> results = new ArrayList<>();
        String sql = "SELECT raw_value, corrected_value, row_number, frequency " +
                     "FROM ticket_corrections WHERE field_type = ? " +
                     "ORDER BY frequency DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fieldType);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(new CorrectionEntry(
                    rs.getString("raw_value"),
                    rs.getString("corrected_value"),
                    rs.getInt("row_number"),
                    rs.getInt("frequency")
                ));
            }
        } catch (DatabaseException | SQLException e) {
            logger.debug("Error al obtener correcciones top: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Busca correcciones similares usando búsqueda parcial.
     * Útil cuando el OCR produce variaciones del mismo texto.
     */
    public String findSimilarCorrection(String fieldType, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;

        String sql = "SELECT corrected_value, frequency FROM ticket_corrections " +
                     "WHERE field_type = ? " +
                     "ORDER BY frequency DESC LIMIT 20";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fieldType);
            ResultSet rs = pstmt.executeQuery();

            String bestMatch = null;
            double bestSim = 0.55; // Umbral mínimo

            while (rs.next()) {
                String storedRaw = rs.getString("corrected_value");
                double sim = app.service.FuzzyMatcher.similarity(
                    app.service.FuzzyMatcher.normalize(rawValue),
                    app.service.FuzzyMatcher.normalize(storedRaw)
                );
                if (sim > bestSim) {
                    bestSim = sim;
                    bestMatch = storedRaw;
                }
            }

            return bestMatch;
        } catch (DatabaseException | SQLException e) {
            logger.debug("Error al buscar corrección similar: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Cuenta el total de correcciones almacenadas.
     */
    public int countAll() {
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ticket_corrections")) {
            if (rs.next()) return rs.getInt(1);
        } catch (DatabaseException | SQLException e) {
            logger.debug("Error al contar correcciones: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Entrada de corrección almacenada.
     */
    public static class CorrectionEntry {
        public final String rawValue;
        public final String correctedValue;
        public final int rowNumber;
        public final int frequency;

        public CorrectionEntry(String rawValue, String correctedValue, int rowNumber, int frequency) {
            this.rawValue = rawValue;
            this.correctedValue = correctedValue;
            this.rowNumber = rowNumber;
            this.frequency = frequency;
        }

        @Override
        public String toString() {
            return String.format("'%s' → '%s' (×%d)", rawValue, correctedValue, frequency);
        }
    }
}
