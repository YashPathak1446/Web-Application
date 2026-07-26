package servlets;
import org.jasypt.util.password.StrongPasswordEncryptor;

public class VerifyPassword {
    private static final StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

    public static boolean checkPassword(String plainTextPassword, String encryptedPassword) {
        return passwordEncryptor.checkPassword(plainTextPassword, encryptedPassword);
    }
}