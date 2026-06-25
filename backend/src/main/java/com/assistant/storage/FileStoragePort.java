package com.assistant.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStoragePort {

    void store(MultipartFile file, String storedFilename);

    boolean isAccessible();
}
