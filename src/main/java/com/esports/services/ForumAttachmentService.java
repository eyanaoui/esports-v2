package com.esports.services;

import com.esports.dao.MessageDao;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public class ForumAttachmentService {
    private static final Set<String> ALLOWED = Set.of("png", "jpg", "jpeg", "pdf", "txt");
    private static final long MAX_SIZE = 5L * 1024L * 1024L;
    private final MessageDao messageDao = new MessageDao();

    public void attachFileToMessage(int messageId, File file) throws Exception {
        validateAttachment(file);
        String path = copyFileToUploadFolder(file);
        messageDao.setAttachmentPath(messageId, path);
    }

    public void validateAttachment(File file) {
        if (file == null || !file.exists()) throw new IllegalArgumentException("Fichier invalide.");
        String name = file.getName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!ALLOWED.contains(ext)) throw new IllegalArgumentException("Extension non autorisée.");
        if (file.length() > MAX_SIZE) throw new IllegalArgumentException("Fichier trop volumineux (max 5 MB).");
    }

    public String copyFileToUploadFolder(File file) throws Exception {
        Path uploadDir = Path.of("uploads", "forum");
        Files.createDirectories(uploadDir);
        String unique = System.currentTimeMillis() + "_" + file.getName().replace(" ", "_");
        Path dest = uploadDir.resolve(unique);
        Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString();
    }
}
