package com.esports.utils;

import javafx.scene.control.TextInputControl;

/**
 * Validation visuelle : classe CSS {@code input-error} (ligne rouge sous le champ).
 */
public final class ValidationHelper {

    private static final String ERR = "input-error";

    private ValidationHelper() {}

    public static void clearFieldError(TextInputControl field) {
        if (field != null) {
            field.getStyleClass().remove(ERR);
        }
    }

    public static void setFieldError(TextInputControl field, boolean invalid) {
        if (field == null) {
            return;
        }
        field.getStyleClass().remove(ERR);
        if (invalid) {
            field.getStyleClass().add(ERR);
        }
    }

    public static void clearFieldErrors(TextInputControl... fields) {
        if (fields == null) {
            return;
        }
        for (TextInputControl f : fields) {
            clearFieldError(f);
        }
    }
}
