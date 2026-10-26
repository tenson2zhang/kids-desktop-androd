package com.kidsdesktop.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void lengthRules() {
        assertFalse(PasswordHasher.isAcceptable(null));
        assertFalse(PasswordHasher.isAcceptable("123"));
        assertFalse(PasswordHasher.isAcceptable("  12  "));
        assertTrue(PasswordHasher.isAcceptable("1234"));
        assertTrue(PasswordHasher.isAcceptable("  1234  "));
        assertTrue(PasswordHasher.isAcceptable("小孩桌面"));
        assertFalse(PasswordHasher.isAcceptable(repeat('a', 33)));
        assertFalse(PasswordHasher.isAcceptable("12\n34"));
    }

    @Test
    public void verifyRoundTripAndRejectsWrongPassword() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hashHex(salt, "desk-2048");
        assertTrue(PasswordHasher.verify(salt, hash, "desk-2048"));
        assertTrue(PasswordHasher.verify(salt, hash, "  desk-2048  "));
        assertFalse(PasswordHasher.verify(salt, hash, "desk-2049"));
        assertFalse(PasswordHasher.verify(salt, hash, null));
        assertFalse(PasswordHasher.verify(null, hash, "desk-2048"));
    }

    @Test
    public void differentSaltsDoNotMatch() {
        String first = PasswordHasher.hashHex(PasswordHasher.newSalt(), "1234");
        String second = PasswordHasher.hashHex(PasswordHasher.newSalt(), "1234");
        assertNotEquals(first, second);
    }

    @Test
    public void goldenHashStaysStable() {
        assertEquals(
                "53b7eb531ccbe85cac1e241bb4e716a96e6e9465d67b3d8b151f95c955b519c1",
                PasswordHasher.hashHex("00112233445566778899aabbccddeeff", "1234"));
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
