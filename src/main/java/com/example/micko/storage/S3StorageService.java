package com.example.micko.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("aws")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String folderPath;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
        this.bucketName = "your-s3-bucket";
        this.folderPath = "your-s3-folder";
    }

    /**
     * List YAML Files from S3
     */
    @Override
    public List<String> listYamlFiles(String folderPath) {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(this.folderPath)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Read YAML File from S3
     */
    @Override
    public InputStream readYamlFile(String fileKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        return s3Client.getObject(getRequest);
    }
    
    /**
     * Save YAML File to S3
     */
    @Override
    public void saveYamlFile(String fileName, String content) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(folderPath + "/" + fileName)
                .build();

        s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length()
        ));
    }
}
