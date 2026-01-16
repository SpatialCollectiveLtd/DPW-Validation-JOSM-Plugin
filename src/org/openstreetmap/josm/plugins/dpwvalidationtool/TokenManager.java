package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Manages secure storage and retrieval of OAuth tokens.
 * Uses AES-256 encryption to protect tokens in JOSM preferences.
 * 
 * @version 3.4.0
 * @since 3.4.0
 */
public class TokenManager {
    
    private static final String PREFIX = "dpw-validation-tool.oauth.";
    private static final String ACCESS_TOKEN_KEY = PREFIX + "access-token";
    private static final String REFRESH_TOKEN_KEY = PREFIX + "refresh-token";
    private static final String TOKEN_EXPIRY_KEY = PREFIX + "token-expiry";
    private static final String USERNAME_KEY = PREFIX + "username";
    private static final String ENCRYPTION_SALT_KEY = PREFIX + "encryption-salt";
    
    // Encryption settings
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    
    // Machine-specific key (derived from system properties)
    private static final String ENCRYPTION_PASSWORD = generateMachineKey();
    
    /**
     * Save access token (encrypted)
     */
    public static void saveAccessToken(String token) {
        if (token == null || token.isEmpty()) {
            Config.getPref().put(ACCESS_TOKEN_KEY, "");
            return;
        }
        
        try {
            String encrypted = encrypt(token);
            Config.getPref().put(ACCESS_TOKEN_KEY, encrypted);
            Logging.debug("DPW OAuth: Access token saved (encrypted)");
        } catch (Exception e) {
            Logging.error("DPW OAuth: Failed to encrypt access token: " + e.getMessage());
            // Store unencrypted as fallback (better than losing the token)
            Config.getPref().put(ACCESS_TOKEN_KEY, token);
        }
    }
    
    /**
     * Load access token (decrypted)
     */
    public static String loadAccessToken() {
        String encrypted = Config.getPref().get(ACCESS_TOKEN_KEY, "");
        if (encrypted.isEmpty()) {
            return null;
        }
        
        try {
            return decrypt(encrypted);
        } catch (Exception e) {
            Logging.warn("DPW OAuth: Failed to decrypt access token, assuming unencrypted: " + e.getMessage());
            // Might be unencrypted (legacy or fallback)
            return encrypted;
        }
    }
    
    /**
     * Save refresh token (encrypted)
     */
    public static void saveRefreshToken(String token) {
        if (token == null || token.isEmpty()) {
            Config.getPref().put(REFRESH_TOKEN_KEY, "");
            return;
        }
        
        try {
            String encrypted = encrypt(token);
            Config.getPref().put(REFRESH_TOKEN_KEY, encrypted);
            Logging.debug("DPW OAuth: Refresh token saved (encrypted)");
        } catch (Exception e) {
            Logging.error("DPW OAuth: Failed to encrypt refresh token: " + e.getMessage());
            Config.getPref().put(REFRESH_TOKEN_KEY, token);
        }
    }
    
    /**
     * Load refresh token (decrypted)
     */
    public static String loadRefreshToken() {
        String encrypted = Config.getPref().get(REFRESH_TOKEN_KEY, "");
        if (encrypted.isEmpty()) {
            return null;
        }
        
        try {
            return decrypt(encrypted);
        } catch (Exception e) {
            Logging.warn("DPW OAuth: Failed to decrypt refresh token, assuming unencrypted: " + e.getMessage());
            return encrypted;
        }
    }
    
    /**
     * Save token expiry time (Unix timestamp in milliseconds)
     */
    public static void saveTokenExpiry(long expiryTime) {
        Config.getPref().putLong(TOKEN_EXPIRY_KEY, expiryTime);
        Logging.debug("DPW OAuth: Token expiry saved: " + expiryTime);
    }
    
    /**
     * Load token expiry time
     */
    public static long loadTokenExpiry() {
        return Config.getPref().getLong(TOKEN_EXPIRY_KEY, 0);
    }
    
    /**
     * Check if access token is expired
     */
    public static boolean isAccessTokenExpired() {
        long expiry = loadTokenExpiry();
        if (expiry == 0) {
            return true; // No expiry set means expired
        }
        
        // Add 5-minute buffer for safety
        long now = System.currentTimeMillis();
        boolean expired = now >= (expiry - 300000);
        
        Logging.debug("DPW OAuth: Token expiry check - now=" + now + ", expiry=" + expiry + ", expired=" + expired);
        return expired;
    }
    
    /**
     * Save authenticated username
     */
    public static void saveUsername(String username) {
        Config.getPref().put(USERNAME_KEY, username != null ? username : "");
        Logging.debug("DPW OAuth: Username saved: " + username);
    }
    
    /**
     * Load authenticated username
     */
    public static String loadUsername() {
        return Config.getPref().get(USERNAME_KEY, null);
    }
    
    /**
     * Clear all stored tokens and user info
     */
    public static void clearAllTokens() {
        Config.getPref().put(ACCESS_TOKEN_KEY, "");
        Config.getPref().put(REFRESH_TOKEN_KEY, "");
        Config.getPref().putLong(TOKEN_EXPIRY_KEY, 0);
        Config.getPref().put(USERNAME_KEY, "");
        Logging.info("DPW OAuth: All tokens cleared");
    }
    
    /**
     * Check if user has valid tokens
     */
    public static boolean hasValidTokens() {
        String accessToken = loadAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        
        if (isAccessTokenExpired()) {
            // Check if we have refresh token
            String refreshToken = loadRefreshToken();
            return refreshToken != null && !refreshToken.isEmpty();
        }
        
        return true;
    }
    
    /**
     * Encrypt a string using AES-256
     */
    private static String encrypt(String plainText) throws Exception {
        byte[] salt = getOrCreateSalt();
        SecretKey key = generateKey(salt);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    /**
     * Decrypt a string using AES-256
     */
    private static String decrypt(String encryptedText) throws Exception {
        byte[] salt = getOrCreateSalt();
        SecretKey key = generateKey(salt);
        
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV and encrypted data
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
        
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    /**
     * Generate encryption key from password and salt
     */
    private static SecretKey generateKey(byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(
            ENCRYPTION_PASSWORD.toCharArray(),
            salt,
            ITERATION_COUNT,
            KEY_LENGTH
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    /**
     * Get or create encryption salt
     */
    private static byte[] getOrCreateSalt() {
        String saltStr = Config.getPref().get(ENCRYPTION_SALT_KEY, "");
        
        if (saltStr.isEmpty()) {
            // Generate new salt
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            saltStr = Base64.getEncoder().encodeToString(salt);
            Config.getPref().put(ENCRYPTION_SALT_KEY, saltStr);
            Logging.debug("DPW OAuth: Generated new encryption salt");
            return salt;
        } else {
            return Base64.getDecoder().decode(saltStr);
        }
    }
    
    /**
     * Generate machine-specific encryption key
     * Uses system properties to create a unique key per machine
     */
    private static String generateMachineKey() {
        StringBuilder key = new StringBuilder();
        
        // Use system properties that are relatively stable
        key.append(System.getProperty("user.name", "default"));
        key.append(System.getProperty("os.name", "unknown"));
        key.append(System.getProperty("os.version", "unknown"));
        
        // Add a fixed component
        key.append("DPW_JOSM_OAUTH_2025");
        
        return key.toString();
    }
    
    /**
     * Get token info for debugging (sanitized)
     */
    public static String getTokenInfo() {
        String accessToken = loadAccessToken();
        String refreshToken = loadRefreshToken();
        long expiry = loadTokenExpiry();
        String username = loadUsername();
        
        StringBuilder info = new StringBuilder();
        info.append("Access Token: ").append(accessToken != null && !accessToken.isEmpty() ? "present (" + accessToken.length() + " chars)" : "none").append("\n");
        info.append("Refresh Token: ").append(refreshToken != null && !refreshToken.isEmpty() ? "present (" + refreshToken.length() + " chars)" : "none").append("\n");
        info.append("Expiry: ").append(expiry > 0 ? new java.util.Date(expiry).toString() : "none").append("\n");
        info.append("Expired: ").append(isAccessTokenExpired()).append("\n");
        info.append("Username: ").append(username != null ? username : "none").append("\n");
        info.append("Has Valid Tokens: ").append(hasValidTokens()).append("\n");
        
        return info.toString();
    }
}
