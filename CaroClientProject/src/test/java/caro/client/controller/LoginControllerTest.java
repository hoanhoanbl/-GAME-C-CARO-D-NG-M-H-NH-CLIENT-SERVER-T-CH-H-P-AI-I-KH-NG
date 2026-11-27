package caro.client.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerTest {
    private static boolean valid(String email) { return LoginController.EMAIL_PATTERN.matcher(email).matches(); }

    @Test
    void acceptsCommonEmails() {
        assertTrue(valid("an@gmail.com"));
        assertTrue(valid("an.nguyen-1@vku.udn.vn"));
        assertTrue(valid("an@vku.travel"), "tên miền cấp cao dài hơn 4 ký tự");
    }

    @Test
    void rejectsBadEmails() {
        assertFalse(valid("an@"));
        assertFalse(valid("an@gmail"));
        assertFalse(valid("an@gmail.c"));
        assertFalse(valid("an gmail.com"));
    }
}
