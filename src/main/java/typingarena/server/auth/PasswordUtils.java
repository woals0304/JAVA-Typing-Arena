package typingarena.server.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtils {
    
    private static final SecureRandom RAND = new SecureRandom();
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // 1. 솔트 생성
    public static String getSalt() {
        byte[] salt = new byte[16];
        RAND.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 2. 비밀번호 해싱
    public static String hashPassword(String password, String salt) {
        char[] chars = password.toCharArray();
        byte[] bytes = Base64.getDecoder().decode(salt);
        
        PBEKeySpec spec = new PBEKeySpec(chars, bytes, ITERATIONS, KEY_LENGTH);
        Arrays.fill(chars, Character.MIN_VALUE);
        try {
            SecretKeyFactory fac = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] securePassword = fac.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(securePassword);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.err.println("비밀번호 해싱 중 오류: " + ex.getMessage());
            return null;
        } finally {
            spec.clearPassword();
        }
    }

    // 3. 비밀번호 검증
    public static boolean verifyPassword(String providedPassword, String storedHash, String salt) {
        String newHash = hashPassword(providedPassword, salt);
        return newHash != null && newHash.equals(storedHash);
    }
}