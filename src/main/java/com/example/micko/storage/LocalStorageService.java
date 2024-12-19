package com.example.micko.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("local")
public class LocalStorageService implements StorageService {

    private final String folderPath;

    public LocalStorageService() {
        this.folderPath = Paths.get("src", "main", "resources", "local-storage").toString();  // Local Folder Path
        createFolderIfNotExists();
    }

    /**
     * Create Local Folder If It Doesn't Exist
     */
    private void createFolderIfNotExists() {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
            System.out.println("Created local storage folder at: " + folderPath);
        }
    }

    /**
     * List All YAML Files from Local Folder
     */
    @Override
    public List<String> listYamlFiles(String folderPath) {
        List<String> files = new ArrayList<>();
        try {
            Files.list(Paths.get(this.folderPath))
                .filter(path -> path.toString().endsWith(".yaml"))
                .forEach(path -> files.add(path.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * Read YAML File from Local Folder
     */
    @Override
    public InputStream readYamlFile(String fileKey) {
        try {
            return new FileInputStream(new File(this.folderPath, fileKey));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read file: " + fileKey);
        }
    }

    /**
     * Save YAML File to Local Folder
     */
    @Override
    public void saveYamlFile(String fileName, String content) {
        File outputFile = new File(this.folderPath, fileName);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(content);
            System.out.println("Saved YAML file: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save file: " + fileName);
        }
    }
}
