package com.esports.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Détecte les noms de colonnes réels de {@code messages_forum} (chaque groupe utilise souvent des noms différents).
 */
public final class MessagesForumMetadata {

    private static final Object LOCK = new Object();
    private static volatile boolean loaded;
    private static String colId = "id";
    private static String colFk = "sujet_id";
    private static String colContent = "contenu";
    private static String colAuthor = null;
    private static String colCreatedAt = null;
    private static final List<String> allColumns = new ArrayList<>();

    private MessagesForumMetadata() {}

    public static void ensureLoaded(Connection con) {
        if (loaded) {
            return;
        }
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            load(con);
            loaded = true;
        }
    }

    private static void load(Connection con) {
        List<String> columns = new ArrayList<>();
        try {
            DatabaseMetaData md = con.getMetaData();
            String catalog = con.getCatalog();
            readColumns(md, catalog, columns);
            if (columns.isEmpty()) {
                readColumns(md, null, columns);
            }
        } catch (SQLException e) {
            System.err.println("MessagesForumMetadata: " + e.getMessage());
            return;
        }
        if (columns.isEmpty()) {
            System.err.println("MessagesForumMetadata: table messages_forum introuvable ou vide (métadonnées).");
            return;
        }
        allColumns.clear();
        allColumns.addAll(columns);
        for (String c : columns) {
            if ("id".equalsIgnoreCase(c)) {
                colId = c;
                break;
            }
        }
        String fk = pickFk(columns);
        if (fk != null) {
            colFk = fk;
        } else {
            System.err.println("MessagesForumMetadata: aucune colonne FK « sujet » détectée. Colonnes=" + columns);
        }
        String content = pickContent(columns);
        if (content != null) {
            colContent = content;
        } else {
            System.err.println("MessagesForumMetadata: aucune colonne de texte détectée. Colonnes=" + columns);
        }
        colAuthor = pickAuthor(columns);
        colCreatedAt = pickCreatedAt(columns);
        System.out.println("messages_forum → colonnes détectées : id=" + colId + ", fk_sujet=" + colFk + ", texte=" + colContent);
    }

    private static void readColumns(DatabaseMetaData md, String catalog, List<String> out) throws SQLException {
        try (ResultSet rs = md.getColumns(catalog, null, "messages_forum", "%")) {
            while (rs.next()) {
                out.add(rs.getString("COLUMN_NAME"));
            }
        }
    }

    private static String pickFk(List<String> columns) {
        String[] preferred = {
                "sujet_id", "id_sujet", "topic_id", "forum_sujet_id", "fk_sujet",
                "idsujet", "idSujet", "forum_id", "sujet"
        };
        for (String p : preferred) {
            for (String c : columns) {
                if (c.equalsIgnoreCase(p)) {
                    return c;
                }
            }
        }
        for (String c : columns) {
            if (c.equalsIgnoreCase(colId)) {
                continue;
            }
            String l = c.toLowerCase();
            if (l.contains("sujet") || l.contains("topic") || l.contains("forum")) {
                return c;
            }
        }
        return null;
    }

    private static String pickContent(List<String> columns) {
        String[] preferred = {
                "contenu", "message", "texte", "body", "content", "text", "msg", "commentaire"
        };
        for (String p : preferred) {
            for (String c : columns) {
                if (c.equalsIgnoreCase(p)) {
                    return c;
                }
            }
        }
        for (String c : columns) {
            if (c.equalsIgnoreCase(colId) || c.equalsIgnoreCase(colFk)) {
                continue;
            }
            String l = c.toLowerCase();
            if (l.contains("conten") || l.contains("mess") || l.contains("text") || l.contains("body")) {
                return c;
            }
        }
        return null;
    }

    private static String pickAuthor(List<String> columns) {
        String[] preferred = {
                "auteur_id", "author_id", "user_id", "id_user", "id_auteur", "membre_id", "utilisateur_id"
        };
        for (String p : preferred) {
            for (String c : columns) {
                if (c.equalsIgnoreCase(p)) {
                    return c;
                }
            }
        }
        return null;
    }

    private static String pickCreatedAt(List<String> columns) {
        String[] preferred = {
                "date_creation", "created_at", "creation_date", "date_creation_message", "message_date"
        };
        for (String p : preferred) {
            for (String c : columns) {
                if (c.equalsIgnoreCase(p)) {
                    return c;
                }
            }
        }
        for (String c : columns) {
            String l = c.toLowerCase();
            if (l.contains("date") && (l.contains("creation") || l.contains("created"))) {
                return c;
            }
        }
        return null;
    }

    /** Identifiant de ligne (clé primaire). */
    public static String idColumn() {
        return colId;
    }

    /** Colonne lien vers le sujet. */
    public static String fkSujetColumn() {
        return colFk;
    }

    /** Colonne texte du message. */
    public static String contentColumn() {
        return colContent;
    }

    /** Colonne auteur optionnelle (null si absente). */
    public static String authorColumn() {
        return colAuthor;
    }

    public static boolean hasColumn(String columnName) {
        if (columnName == null) return false;
        for (String c : allColumns) {
            if (c.equalsIgnoreCase(columnName)) return true;
        }
        return false;
    }

    /** Colonne date de création optionnelle (null si absente). */
    public static String createdAtColumn() {
        return colCreatedAt;
    }

    private static String quoteIdent(String name) {
        if (name == null) {
            return "`id`";
        }
        return "`" + name.replace("`", "") + "`";
    }

    public static String qId() {
        return quoteIdent(colId);
    }

    public static String qFk() {
        return quoteIdent(colFk);
    }

    public static String qContent() {
        return quoteIdent(colContent);
    }

    public static String qAuthor() {
        return quoteIdent(colAuthor);
    }

    public static String qCreatedAt() {
        return quoteIdent(colCreatedAt);
    }

    /** Retourne le premier nom de colonne existant parmi les candidats (ou null). */
    public static String firstExistingColumn(String... candidates) {
        if (candidates == null) return null;
        for (String cand : candidates) {
            if (cand == null || cand.isBlank()) continue;
            for (String c : allColumns) {
                if (c.equalsIgnoreCase(cand)) {
                    return c;
                }
            }
        }
        return null;
    }

    public static String q(String columnName) {
        return quoteIdent(columnName);
    }
}
