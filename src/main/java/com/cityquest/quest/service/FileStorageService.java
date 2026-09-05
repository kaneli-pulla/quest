package com.cityquest.quest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadRoot;


    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        uploadRoot = Paths
                .get(uploadDir)
                        .toAbsolutePath()
                        .normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать папку для фотографий", e);
        }
    }

    public String saveImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Необходимо выбрать фотографию");
        }
        validateFolder(folder);
        String extension = getExtension(file.getContentType());
        Path directory = uploadRoot.resolve(folder).normalize();
        if (!directory.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Некорректная папка для файла");
        }
        try {
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path target = directory
                            .resolve(filename)
                            .normalize();
            if (!target.startsWith(directory)) {
                throw new IllegalArgumentException("Некорректный путь к файлу");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/" + folder + "/" + filename;
            deleteOnRollback(url);
            return url;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить фотографию", e);
        }
    }

    public void deleteFileAfterCommit(String url) {
        if (!isManagedUploadUrl(url)) {
            return;
        }
        if (canRegisterTransactionSynchronization()) {
            TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    deleteQuietly(url);
                                }
                            }
                            );

        } else {
            deleteFile(url);
        }
    }

    public void deleteFilesAfterCommit(Collection<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        List<String> files = urls.stream()
                        .filter(this::isManagedUploadUrl)
                        .distinct()
                        .toList();
        if (files.isEmpty()) {
            return;
        }
        if (canRegisterTransactionSynchronization()) {
            TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    for (String file : files) {
                                        deleteQuietly(file);
                                    }
                                }
                            }
                            );

        } else {
            for (String file : files) {
                deleteFile(file);
            }
        }
    }

    public void deleteFile(String url) {
        if (!isManagedUploadUrl(url)) {
            return;
        }
        Path file = resolveUploadPath(url);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось удалить файл", e);
        }
    }

    private void deleteOnRollback(String url) {
        if (!canRegisterTransactionSynchronization()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                                    deleteQuietly(url);
                                }
                            }
                        }
                );
    }

    private void deleteQuietly(String url) {
        try {
            deleteFile(url);
        } catch (RuntimeException e) {
            log.warn("Не удалось удалить файл {}", url, e);
        }
    }

    private boolean canRegisterTransactionSynchronization() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }

    private boolean isManagedUploadUrl(String url) {
        return url != null
                && !url.isBlank()
                && url.startsWith("/uploads/");
    }

    private Path resolveUploadPath(String url) {
        String relativePath = url.substring("/uploads/".length());
        Path file = uploadRoot.resolve(relativePath).normalize();
        if (!file.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Некорректный путь к файлу");
        }
        return file;
    }

    private void validateFolder(String folder) {
        if (folder == null || !folder.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Некорректная папка для файла");
        }
    }

    private String getExtension(String contentType) {
        if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        throw new IllegalArgumentException(
                "Разрешены только JPEG, PNG и WebP"
        );
    }
}