package com.kidsdesktop.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** 保护密码只保存盐和摘要，不保存原文。 */
public final class PasswordHasher {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 32;
    static final int ITERATIONS = 10000;

    private PasswordHasher() {
    }

    public static boolean isAcceptable(String password) {
        if (password == null) {
            return false;
        }
        String trimmed = password.trim();
        int length = trimmed.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '\n' || c == '\r') {
                return false;
            }
        }
        return true;
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return toHex(salt);
    }

    public static String hashHex(String saltHex, String password) {
        String normalized = password == null ? "" : password.trim();
        return toHex(hash(fromHex(saltHex), normalized));
    }

    public static boolean verify(String saltHex, String expectedHashHex, String password) {
        if (saltHex == null || expectedHashHex == null || password == null) {
            return false;
        }
        byte[] expected = fromHex(expectedHashHex);
        byte[] actual = fromHex(hashHex(saltHex, password));
        return constantTimeEquals(expected, actual);
    }

    static byte[] hash(byte[] salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] input = new byte[salt.length + passwordBytes.length];
            System.arraycopy(salt, 0, input, 0, salt.length);
            System.arraycopy(passwordBytes, 0, input, salt.length, passwordBytes.length);
            byte[] out = input;
            for (int i = 0; i < ITERATIONS; i++) {
                out = digest.digest(out);
            }
            return out;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left == null || right == null || left.length != right.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length; i++) {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }

    static String toHex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            hex[i * 2] = digits[value >>> 4];
            hex[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(hex);
    }

    static byte[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[0];
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return new byte[0];
            }
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
