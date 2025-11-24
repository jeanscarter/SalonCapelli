package app.repository;

import app.db.DatabaseConnection;
import app.model.Cliente;
import app.model.TipoCabello;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepositorySQLite implements ClienteRepository {

    @Override
    public void create(Cliente c) throws Exception {
        String sql = """
            INSERT INTO clientes (cedula, nombre_completo, telefono, direccion, 
            tipo_cabello, tipo_extensiones, fecha_cumpleanos, fecha_ultimo_tinte, 
            fecha_ultimo_quimico, fecha_ultima_keratina, fecha_ultimo_mantenimiento) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            mapClienteToStmt(c, pstmt);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void update(Cliente c) throws Exception {
        String sql = """
            UPDATE clientes SET cedula=?, nombre_completo=?, telefono=?, direccion=?, 
            tipo_cabello=?, tipo_extensiones=?, fecha_cumpleanos=?, fecha_ultimo_tinte=?, 
            fecha_ultimo_quimico=?, fecha_ultima_keratina=?, fecha_ultimo_mantenimiento=? 
            WHERE id=?
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            mapClienteToStmt(c, pstmt);
            pstmt.setInt(12, c.getId()); // El ID va al final en el WHERE
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Cliente> findAll() throws Exception {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes ORDER BY nombre_completo";
        
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                lista.add(mapResultSetToCliente(rs));
            }
        }
        return lista;
    }

    @Override
    public Cliente findByCedula(String cedula) throws Exception {
        String sql = "SELECT * FROM clientes WHERE cedula = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, cedula);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCliente(rs);
                }
            }
        }
        return null;
    }

    // --- Helpers para evitar código repetido ---

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

    private Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setCedula(rs.getString("cedula"));
        c.setNombreCompleto(rs.getString("nombre_completo"));
        c.setTelefono(rs.getString("telefono"));
        c.setDireccion(rs.getString("direccion"));
        
        String tipo = rs.getString("tipo_cabello");
        if (tipo != null) c.setTipoCabello(TipoCabello.valueOf(tipo));
        
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
}