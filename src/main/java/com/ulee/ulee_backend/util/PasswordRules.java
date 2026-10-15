package com.ulee.ulee_backend.util;

public final class PasswordRules {

    public static final String MESSAGE =
            "Your password must be at least 8 characters and include a letter and a number.";

    private PasswordRules() { }

    /** Returns null when the password is acceptable, otherwise the error message. */
    public static String check(String password) {
        if (password == null || password.length() < 8) return MESSAGE;
        boolean letter = false, digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            else if (Character.isDigit(c)) digit = true;
        }
        return (letter && digit) ? null : MESSAGE;
    }
}