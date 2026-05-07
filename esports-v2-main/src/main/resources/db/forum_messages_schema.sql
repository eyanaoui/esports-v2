-- Référence : l'application détecte automatiquement les noms de colonnes (FK vers sujet, texte du message).
-- En cas de problème : DESCRIBE messages_forum; et vérifiez la console (ligne "messages_forum → colonnes détectées").

CREATE TABLE IF NOT EXISTS messages_forum (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_sujet INT NOT NULL,
    contenu TEXT NOT NULL,
    CONSTRAINT fk_messages_sujet FOREIGN KEY (id_sujet)
        REFERENCES sujets_forum(id) ON DELETE CASCADE
);
