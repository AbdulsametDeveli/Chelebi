package com.gizlirotalar.app.utils;

import com.gizlirotalar.app.BuildConfig;

/**
 * Yönetici e-posta adresleri koda gömülmez; local.properties içindeki
 * ADMIN_EMAILS değerinden (virgülle ayrılmış) derleme sırasında okunur.
 */
public final class AdminConfig {

    private AdminConfig() {}

    public static boolean isAdminEmail(String email) {
        if (email == null || BuildConfig.ADMIN_EMAILS == null) return false;
        String target = email.trim().toLowerCase();
        for (String admin : BuildConfig.ADMIN_EMAILS.split(",")) {
            if (!admin.trim().isEmpty() && admin.trim().toLowerCase().equals(target)) {
                return true;
            }
        }
        return false;
    }
}
