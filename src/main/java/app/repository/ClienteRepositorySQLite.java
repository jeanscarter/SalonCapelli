package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.exception.cliente.ClienteDuplicadoException;
import app.exception.cliente.ClienteException;
import app.exception.cliente.ClienteNotFoundException;
import app.model.Cliente;
import app.model.TipoCabello;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de clientes
 * Patrón: Repository + Template Method
 */
public class ClienteRepositorySQLite implements ClienteRepository {

    private static final Logger logger = LoggerFactory.getLogger(ClienteRepositorySQLite.class);
    
    // Queries SQL como constantes (mejor mantenibilidad)
    private static final String SQL_CREATE = """
        INSERT INTO clientes (cedula, nombre_completo, telefono, direccion, 
        tipo_cabello, tipo_extensiones, fecha_cumpleanos, fecha_ultimo_tinte, 
        fecha_ultimo_quimico, fecha_ultima_keratina, fecha_ultimo_mantenimiento) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
    
    private static final String SQL_UPDATE = """
        UPDATE clientes SET cedula=?, nombre_completo=?, telefono=?, direccion=?, 
        tipo_cabello=?, tipo_extensiones=?, fecha_cumpleanos=?, fecha_ultimo_tinte=?, 
        fecha_ultimo_quimico=?, fecha_ultima_keratina=?, fecha_ultimo_mantenimiento=?,
        fecha_modificacion=CURRENT_TIMESTAMP
        WHERE id=?
    """;
    
    private static final String SQL_DELETE = "DELETE FROM clientes WHERE id = ?";
    
    private static final String SQL_FIND_ALL = 
        "SELECT * FROM clientes ORDER BY nombre_completo";
    
    private static final String SQL_FIND_BY_CEDULA = 
        "SELECT * FROM clientes WHERE cedula = ?";
    
    private static final String SQL_FIND_BY_ID = 
        "SELECT * FROM clientes WHERE id = ?";
    
    private static final String SQL_SEARCH_BY_NOMBRE = 
        "SELECT * FROM clientes WHERE nombre_completo LIKE ? ORDER BY nombre_completo LIMIT 100";
    
    private static final String SQL_EXISTS_BY_CEDULA = 
        "SELECT COUNT(*) FROM clientes WHERE cedula = ?";
    
    private static final String SQL_COUNT = 
        "SELECT COUNT(*) FROM clientes";

    @Override
    public void create(Cliente c) throws ClienteDuplicadoException, DatabaseException, ClienteException {
        logger.info("Intentando crear cliente: {}", c.getCedula());
        
        // Validar duplicados ANTES de insertar
        if (existsByCedula(c.getCedula())) {
            logger.warn("Intento de crear cliente duplicado: {}", c.getCedula());
            throw new ClienteDuplicadoException(c.getCedula());
        }
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            
            logger.debug("Mapeando cliente a PreparedStatement");
            mapClienteToStmt(c, pstmt);
            
            int affected = pstmt.executeUpdate();
            logger.debug("Filas afectadas: {}", affected);
            
            // Obtener el ID generado
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                    logger.info("✓ Cliente creado exitosamente con ID: {} - Cédula: {}", c.getId(), c.getCedula());
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error SQL al crear cliente: {}", e.getMessage(), e);
            
            // Detectar violación de constraint UNIQUE
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                throw new ClienteDuplicadoException(c.getCedula());
            }
            
            throw DatabaseException.queryFailed("CREATE CLIENTE", e);
        }
    }

    @Override
    public void update(Cliente c) throws ClienteNotFoundException, DatabaseException, ClienteException {
        logger.info("Actualizando cliente ID: {} - Cédula: {}", c.getId(), c.getCedula());
        
        // Verificar que el cliente existe
        if (!existsById(c.getId())) {
            logger.warn("Intento de actualizar cliente inexistente: ID {}", c.getId());
            throw ClienteNotFoundException.byId(c.getId());
        }
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
            
            logger.debug("Mapeando cliente actualizado a PreparedStatement");
            mapClienteToStmt(c, pstmt);
            pstmt.setInt(12, c.getId()); // WHERE id=?
            
            int affected = pstmt.executeUpdate();
            logger.debug("Filas actualizadas: {}", affected);
            
            if (affected == 0) {
                logger.warn("No se actualizó ninguna fila para el cliente ID: {}", c.getId());
                throw ClienteNotFoundException.byId(c.getId());
            }
            
            logger.info("✓ Cliente actualizado exitosamente: ID {} - {}", c.getId(), c.getNombreCompleto());
            
        } catch (SQLException e) {
            logger.error("Error SQL al actualizar cliente: {}", e.getMessage(), e);
            throw DatabaseException.queryFailed("UPDATE CLIENTE", e);
        }
    }

    @Override
    public void delete(int id) throws ClienteNotFoundException, DatabaseException {
        logger.info("Eliminando cliente ID: {}", id);
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {
            
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            
            if (affected == 0) {
                logger.warn("No se encontró cliente con ID: {} para eliminar", id);
                throw ClienteNotFoundException.byId(id);
            }
            
            logger.info("✓ Cliente eliminado exitosamente: ID {}", id);
            
        } catch (SQLException e) {
            logger.error("Error SQL al eliminar cliente: {}", e.getMessage(), e);
            throw DatabaseException.queryFailed("DELETE CLIENTE", e);
        }
    }

    @Override
    public List<Cliente> findAll() throws DatabaseException {
        logger.debug("Obteniendo todos los clientes");
        
        List<Cliente> lista = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
            
            while (rs.next()) {
                lista.add(mapResultSetToCliente(rs));
            }
            
            logger.info("✓ Se obtuvieron {} clientes", lista.size());
            return lista;
            
        } catch (SQLException e) {
            logger.error("Error SQL al obtener todos los clientes", e);
            throw DatabaseException.queryFailed("SELECT ALL CLIENTES", e);
        }
    }

    @Override
    public Cliente findByCedula(String cedula) throws DatabaseException {
        logger.debug("Buscando cliente por cédula: {}", cedula);
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_CEDULA)) {
            
            pstmt.setString(1, cedula);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = mapResultSetToCliente(rs);
                    logger.debug("✓ Cliente encontrado: ID {} - {}", cliente.getId(), cliente.getNombreCompleto());
                    return cliente;
                }
            }
            
            logger.debug("No se encontró cliente con cédula: {}", cedula);
            return null;
            
        } catch (SQLException e) {
            logger.error("Error SQL al buscar por cédula: {}", cedula, e);
            throw DatabaseException.queryFailed("FIND BY CEDULA", e);
        }
    }

    @Override
    public Cliente findById(int id) throws ClienteNotFoundException, DatabaseException {
        logger.debug("Buscando cliente por ID: {}", id);
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = mapResultSetToCliente(rs);
                    logger.debug("✓ Cliente encontrado: {}", cliente.getNombreCompleto());
                    return cliente;
                }
            }
            
            logger.warn("No se encontró cliente con ID: {}", id);
            throw ClienteNotFoundException.byId(id);
            
        } catch (SQLException e) {
            logger.error("Error SQL al buscar por ID: {}", id, e);
            throw DatabaseException.queryFailed("FIND BY ID", e);
        }
    }

    @Override
    public List<Cliente> searchByNombre(String nombre) throws DatabaseException {
        logger.debug("Buscando clientes por nombre: {}", nombre);
        
        List<Cliente> lista = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SEARCH_BY_NOMBRE)) {
            
            pstmt.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToCliente(rs));
                }
            }
            
            logger.info("✓ Búsqueda completada: {} resultados para '{}'", lista.size(), nombre);
            return lista;
            
        } catch (SQLException e) {
            logger.error("Error SQL al buscar por nombre: {}", nombre, e);
            throw DatabaseException.queryFailed("SEARCH BY NOMBRE", e);
        }
    }

    @Override
    public boolean existsByCedula(String cedula) throws DatabaseException {
        logger.debug("Verificando existencia de cédula: {}", cedula);
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_EXISTS_BY_CEDULA)) {
            
            pstmt.setString(1, cedula);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean exists = rs.getInt(1) > 0;
                    logger.debug("Cédula {} existe: {}", cedula, exists);
                    return exists;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error SQL al verificar existencia de cédula: {}", cedula, e);
            throw DatabaseException.queryFailed("EXISTS BY CEDULA", e);
        }
    }

    @Override
    public int count() throws DatabaseException {
        logger.debug("Contando total de clientes");
        
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {
            
            if (rs.next()) {
                int total = rs.getInt(1);
                logger.info("Total de clientes: {}", total);
                return total;
            }
            
            return 0;
            
        } catch (SQLException e) {
            logger.error("Error SQL al contar clientes", e);
            throw DatabaseException.queryFailed("COUNT CLIENTES", e);
        }
    }

    // ========== Métodos Helper (Template Method Pattern) ==========

    /**
     * Mapea un Cliente a un PreparedStatement
     * Template Method para reutilizar en CREATE y UPDATE
     */
    private void mapClienteToStmt(Cliente c, PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, c.getCedula());
        pstmt.setString(2, c.getNombreCompleto());
        pstmt.setString(3, c.getTelefono());
        pstmt.setString(4, c.getDireccion());
        pstmt.setString(5, c.getTipoCabello() != null ? c.getTipoCabello().name() : null);
        pstmt.setString(6, c.getTipoExtensiones());
        pstmt.setString(7, dateToString(c.getFechaCumpleanos()));
        pstmt.setString(8, dateToString(c.getFechaUltimoTinte()));
        pstmt.setString(9, dateToString(c.getFechaUltimoQuimico()));
        pstmt.setString(10, dateToString(c.getFechaUltimaKeratina()));
        pstmt.setString(11, dateToString(c.getFechaUltimoMantenimiento()));
    }

    /**
     * Mapea un ResultSet a un objeto Cliente
     */
    private Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setCedula(rs.getString("cedula"));
        c.setNombreCompleto(rs.getString("nombre_completo"));
        c.setTelefono(rs.getString("telefono"));
        c.setDireccion(rs.getString("direccion"));
        
        String tipo = rs.getString("tipo_cabello");
        if (tipo != null) {
            c.setTipoCabello(TipoCabello.valueOf(tipo));
        }
        
        c.setTipoExtensiones(rs.getString("tipo_extensiones"));
        c.setFechaCumpleanos(stringToDate(rs.getString("fecha_cumpleanos")));
        c.setFechaUltimoTinte(stringToDate(rs.getString("fecha_ultimo_tinte")));
        c.setFechaUltimoQuimico(stringToDate(rs.getString("fecha_ultimo_quimico")));
        c.setFechaUltimaKeratina(stringToDate(rs.getString("fecha_ultima_keratina")));
        c.setFechaUltimoMantenimiento(stringToDate(rs.getString("fecha_ultimo_mantenimiento")));
        
        return c;
    }

    private String dateToString(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private LocalDate stringToDate(String dateStr) {
        return (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : null;
    }
    
    /**
     * Verifica si existe un cliente por ID
     */
    private boolean existsById(int id) throws DatabaseException {
        try {
            findById(id);
            return true;
        } catch (ClienteNotFoundException e) {
            return false;
        }
    }
}