package app.service;

import app.model.Usuario;
import app.repository.UsuarioRepository;
import app.repository.UsuarioRepositorySQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Servicio de autenticación del sistema.
 *
 * CORRECCIÓN #13: Eliminado SQL directo — ahora delega a UsuarioRepository.
 * La verificación del hash sigue en esta capa (servicio), no en el repositorio.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static Usuario currentUser;
    private static final UsuarioRepository usuarioRepo = new UsuarioRepositorySQLite();

    public static Usuario getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }

    /**
     * Autentica un usuario por username y contraseña.
     * CORRECCIÓN #13: Usa UsuarioRepository en lugar de SQL directo.
     */
    public static boolean authenticate(String username, String password) {
        try {
            Usuario u = usuarioRepo.findByUsername(username);
            if (u == null || !u.isActivo()) {
                return false;
            }

            String hashed = hashPassword(password);
            if (hashed.equals(u.getPasswordHash())) {
                currentUser = u;
                return true;
            }
        } catch (Exception e) {
            logger.error("Error durante autenticación", e);
        }
        return false;
    }

    /**
     * Verifica si una contraseña corresponde a algún usuario ADMIN activo.
     * CORRECCIÓN #13: Usa UsuarioRepository para buscar admins.
     */
    public static boolean authenticateAdmin(String password) {
        String hashed = hashPassword(password);
        try {
            for (Usuario u : usuarioRepo.findAll()) {
                if ("ADMIN".equals(u.getRol()) && u.isActivo() && hashed.equals(u.getPasswordHash())) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error durante autenticación admin", e);
        }
        return false;
    }

    /**
     * Hash SHA-256 de una contraseña.
     * Nota: Se mantiene SHA-256 sin salt por compatibilidad con datos existentes.
     * Una migración futura a PBKDF2 requeriría migración de hashes en DB.
     */
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
