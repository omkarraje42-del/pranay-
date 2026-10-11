package com.library.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$"
    );

    // ISBN-10 and ISBN-13 allowing hyphens
    private static final Pattern ISBN_PATTERN = Pattern.compile(
            "^(?:ISBN(?:-1[03])?:?\\s*)?(?=[0-9X]{10}$|(?=(?:[0-9]+[-\\s]){3})[-\\s0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[-\\s]){4})[-\\s0-9]{17}$)(?:97[89][-\\s]?)?[0-9]{1,5}[-\\s]?[0-9]+[-\\s]?[0-9]+[-\\s]?[0-9X]$"
    );

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return true; // Optional field in some contexts
        }
        String clean = isbn.trim().replace("-", "").replace(" ", "");
        return clean.length() == 10 || clean.length() == 13;
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.trim();
    }
}
