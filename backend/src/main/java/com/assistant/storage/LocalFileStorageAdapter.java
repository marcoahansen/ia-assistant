package com.assistant.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path uploadDir;

    public LocalFileStorageAdapter(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    @Override
    public void store(MultipartFile file, String storedFilename) {
        try {
            Path target = uploadDir.resolve(storedFilename).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new SecurityException("Path traversal detected: " + storedFilename);
            }
            file.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFilename, e);
        }
    }

    @Override
    public boolean isAccessible() {
        return Files.isDirectory(uploadDir) && Files.isWritable(uploadDir);
    }
}
