package app.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utilidad de Cifrado Fuerte AES-256 GCM (Galois/Counter Mode) con autenticación de integridad.
 * 
 * Protege todas las claves de API y credenciales almacenadas en la base de datos local SQLite,
 * asegurando que no queden expuestas en texto plano si el archivo .db es inspeccionado externamente.
 */
public class CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH_BIT = 256;

    // Semilla interna de aplicación para derivación de claves maestras
    private static final String APP_SECRET_SEED = "Capelli#Salon$2026@SecKey&EncAES256GCM";

    private static final SecureRandom secureRandom = new SecureRandom();

    private CryptoUtil() {}

    /**
     * Cifra un texto en plano utilizando AES-256 GCM.
     * 
     * @param plainText Texto a proteger
     * @return Cadena Base64 con el Salt, IV y Ciphertext autenticado
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        try {
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            secureRandom.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            SecretKey secretKey = deriveKey(APP_SECRET_SEED.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Empaquetar Salt (16) + IV (12) + CipherText (variable)
            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
            byteBuffer.put(salt);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return "ENC:" + Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            logger.error("Error al cifrar información sensible", e);
            throw new RuntimeException("Fallo en el módulo de seguridad criptográfico", e);
        }
    }

    /**
     * Descifra un texto cifrado con AES-256 GCM.
     * Si el texto no está cifrado (legacy plain text), lo retorna tal cual para compatibilidad.
     * 
     * @param cipherText Texto cifrado con prefijo "ENC:" o Base64
     * @return Texto en plano descifrado
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return cipherText;
        }

        // Si no tiene el prefijo de cifrado, es una clave previa en texto plano
        if (!cipherText.startsWith("ENC:")) {
            return cipherText;
        }

        try {
            String rawBase64 = cipherText.substring(4);
            byte[] decoded = Base64.getDecoder().decode(rawBase64);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] salt = new byte[SALT_LENGTH_BYTE];
            byteBuffer.get(salt);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            SecretKey secretKey = deriveKey(APP_SECRET_SEED.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(encrypted);
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Error al descifrar información sensible", e);
            return cipherText; // Fallback seguro
        }
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH_BIT);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
