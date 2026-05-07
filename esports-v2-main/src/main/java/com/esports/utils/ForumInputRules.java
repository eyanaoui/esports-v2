package com.esports.utils;

import java.util.regex.Pattern;

/**
 * Règles de saisie forum (sujets + messages) : longueurs, début interdit par un chiffre,
 * présence d’au moins une lettre, pas uniquement des chiffres pour le titre.
 */
public final class ForumInputRules {

    private static final Pattern TITLE_ONLY_DIGITS = Pattern.compile("^\\d+$");

    private ForumInputRules() {}

    /**
     * @param titre          texte déjà {@code trim()}
     * @param minNoSpaceLen longueur minimale après suppression des espaces
     * @param maxLen         longueur maximale
     * @return message d’erreur, ou {@code null} si valide
     */
    public static String validateTopicTitle(String titre, int minNoSpaceLen, int maxLen) {
        if (titre == null || titre.isEmpty()) {
            return "Le titre est obligatoire.";
        }
        String compact = titre.replaceAll("\\s+", "");
        if (compact.length() < minNoSpaceLen) {
            return "Le titre doit comporter au moins " + minNoSpaceLen + " caractères (hors espaces).";
        }
        if (titre.length() > maxLen) {
            return "Le titre ne peut pas dépasser " + maxLen + " caractères.";
        }
        int first = titre.codePointAt(0);
        if (Character.isDigit(first)) {
            return "Le titre ne peut pas commencer par un chiffre.";
        }
        if (TITLE_ONLY_DIGITS.matcher(compact).matches()) {
            return "Le titre ne peut pas être uniquement composé de chiffres.";
        }
        if (!containsLetter(titre)) {
            return "Le titre doit contenir au moins une lettre.";
        }
        return null;
    }

    /**
     * @param contenu texte déjà {@code trim()}
     */
    public static String validateTopicContent(String contenu, int minLen, int maxLen) {
        if (contenu == null || contenu.isEmpty()) {
            return "Le contenu est obligatoire.";
        }
        if (contenu.length() < minLen) {
            return "Le contenu doit comporter au moins " + minLen + " caractères.";
        }
        if (contenu.length() > maxLen) {
            return "Le contenu ne peut pas dépasser " + maxLen + " caractères.";
        }
        int first = contenu.codePointAt(0);
        if (Character.isDigit(first)) {
            return "Le contenu ne peut pas commencer par un chiffre.";
        }
        if (!containsLetter(contenu)) {
            return "Le contenu doit contenir au moins une lettre.";
        }
        return null;
    }

    /**
     * @param texte texte déjà {@code trim()}
     */
    public static String validateReply(String texte, int minLen, int maxLen) {
        if (texte == null || texte.isEmpty()) {
            return "Le message ne peut pas être vide.";
        }
        if (texte.length() < minLen) {
            return "Le message doit comporter au moins " + minLen + " caractères.";
        }
        if (texte.length() > maxLen) {
            return "Le message ne peut pas dépasser " + maxLen + " caractères.";
        }
        int first = texte.codePointAt(0);
        if (Character.isDigit(first)) {
            return "Le message ne peut pas commencer par un chiffre.";
        }
        if (!containsLetter(texte)) {
            return "Le message doit contenir au moins une lettre.";
        }
        return null;
    }

    private static boolean containsLetter(String s) {
        return s.codePoints().anyMatch(Character::isLetter);
    }
}
