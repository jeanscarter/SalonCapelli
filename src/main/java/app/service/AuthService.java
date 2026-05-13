package app.service;

import app.db.DatabaseConnection;
import app.model.Usuario;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuthService {

    private static Usuario currentUser;

    public static Usuario getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean authenticate(String username, String password) {
        String hashed = hashPassword(password);
        String sql = "SELECT * FROM usuarios WHERE username = ? AND password_hash = ? AND activo = 1";
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, username);
            pstmt.setString(2, hashed);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setRol(rs.getString("rol"));
                    u.setActivo(rs.getInt("activo") == 1);
                    String dateStr = rs.getString("fecha_creacion");
                    if (dateStr != null) {
                        u.setFechaCreacion(LocalDateTime.parse(dateStr.replace(" ", "T")));
                    }
                    currentUser = u;
                    return true;
                }
            }
        } catch (SQLException | app.exception.DatabaseException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean authenticateAdmin(String password) {
        String hashed = hashPassword(password);
        String sql = "SELECT * FROM usuarios WHERE rol = 'ADMIN' AND password_hash = ? AND activo = 1";
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, hashed);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException | app.exception.DatabaseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
