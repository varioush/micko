package com.example.micko.storage;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    List<String> listYamlFiles(String folderPath);

    InputStream readYamlFile(String fileKey);

    void saveYamlFile(String fileName, String content);
}
