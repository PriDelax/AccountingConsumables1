package com.example.industrialpracticeuchet;

import java.util.prefs.Preferences;

public class SessionManager {
    private static final String PREFS_NODE = "/com/example/industrialpracticeuchet";
    private static final String KEY_CURRENT_USER = "current_user";
    private static Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    public static void setCurrentUser(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            prefs.remove(KEY_CURRENT_USER);
        } else {
            prefs.put(KEY_CURRENT_USER, userName.trim());
        }
    }

    public static String getCurrentUser() {
        return prefs.get(KEY_CURRENT_USER, null);
    }

    public static void clearSession() {
        prefs.remove(KEY_CURRENT_USER);
    }
}
