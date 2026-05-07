package com.esports;

import com.esports.models.User;

public class AppState {

    private static boolean darkMode = false;
    private static User currentUser;

    private AppState() {
        // Utility class
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clearSession() {
        currentUser = null;
    }

    public static void logout() {
        clearSession();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}