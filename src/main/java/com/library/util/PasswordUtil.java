package com.library.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        String pwd = (args.length > 0) ? args[0] : "Admin@123";
        System.out.println("Password: " + pwd);
        System.out.println("BCrypt: " + hashPassword(pwd));
    }
}
