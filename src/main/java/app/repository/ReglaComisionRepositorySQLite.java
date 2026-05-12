package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.ReglaComision;
import app.model.Trabajadora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReglaComisionRepositorySQLite implements ReglaComisionRepository {

    private static final Logger logger = LoggerFactory.getLogger(ReglaComisionRepositorySQLite.class);
    
    private final TrabajadoraRepository trabajadoraRepository;

    public ReglaComisionRepositorySQLite(TrabajadoraRepository trabajadoraRepository) {
        this.trabajadoraRepository = trabajadoraRepository;
    }

    private static final String SQL_CREATE = 
        "INSERT INTO reglas_comision (trabajadora_id, categoria_servicio, porcentaje_comision) VALUES (?, ?, ?)";
    
    private static final String SQL_UPDATE = 
        "UPDATE reglas_comision SET trabajadora_id=?, categoria_servicio=?, porcentaje_comision=? WHERE id=?";
        
    private static final String SQL_DELETE = 
        "DELETE FROM reglas_comision WHERE id=?";
        
    private static final String SQL_FIND_ALL = 
        "SELECT * FROM reglas_comision";
        
    private static final String SQL_FIND_BY_ID = 
        "SELECT * FROM reglas_comision WHERE id=?";

    @Override
    public void create(ReglaComision regla) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, regla.getTrabajadoraId());
            pstmt.setString(2, regla.getCategoriaServicio());
            pstmt.setDouble(3, regla.getPorcentajeComision());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    regla.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new DatabaseException("Ya existe una regla de comisión para esta trabajadora y categoría.", e);
            }
            throw DatabaseException.queryFailed("CREATE REGLA_COMISION", e);
        }
    }

    @Override
    public void update(ReglaComision regla) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
            
            pstmt.setInt(1, regla.getTrabajadoraId());
            pstmt.setString(2, regla.getCategoriaServicio());
            pstmt.setDouble(3, regla.getPorcentajeComision());
            pstmt.setInt(4, regla.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
             if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new DatabaseException("Ya existe una regla de comisión para esta trabajadora y categoría.", e);
            }
            throw DatabaseException.queryFailed("UPDATE REGLA_COMISION", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("DELETE REGLA_COMISION", e);
        }
    }

    @Override
    public List<ReglaComision> findAll() throws DatabaseException {
        List<ReglaComision> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
             
             while (rs.next()) {
                 lista.add(mapResultSetToRegla(rs));
             }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_ALL REGLAS_COMISION", e);
        }
        return lista;
    }

    @Override
    public ReglaComision findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
             
             pstmt.setInt(1, id);
             try (ResultSet rs = pstmt.executeQuery()) {
                 if (rs.next()) {
                     return mapResultSetToRegla(rs);
                 }
             }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_ID REGLA_COMISION", e);
        }
        return null;
    }
    
    private ReglaComision mapResultSetToRegla(ResultSet rs) throws SQLException {
        ReglaComision regla = new ReglaComision();
        regla.setId(rs.getInt("id"));
        regla.setTrabajadoraId(rs.getInt("trabajadora_id"));
        regla.setCategoriaServicio(rs.getString("categoria_servicio"));
        regla.setPorcentajeComision(rs.getDouble("porcentaje_comision"));
        
        try {
            Trabajadora t = trabajadoraRepository.findById(regla.getTrabajadoraId());
            regla.setTrabajadora(t);
        } catch (Exception e) {
            logger.error("Error al buscar trabajadora para regla comision", e);
        }
        return regla;
    }
}
