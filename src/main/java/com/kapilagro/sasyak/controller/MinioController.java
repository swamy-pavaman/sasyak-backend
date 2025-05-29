package com.kapilagro.sasyak.controller;


import com.kapilagro.sasyak.model.PresignedUrlRequest;
import com.kapilagro.sasyak.services.MinioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/minio")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN','SUPERVISOR')")
public class MinioController {

    private final MinioService minioService;

    public MinioController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/presigned-url/upload")
    public ResponseEntity<Map<String, Object>> generateUploadPresignedUrls(
            @RequestBody PresignedUrlRequest request) {

        try {
            Map<String, String> presignedUrls = new HashMap<>();

            // Build folder path - ensure it ends with / if provided
            String folderPath = "";
            if (request.getFolder() != null && !request.getFolder().trim().isEmpty()) {
                folderPath = request.getFolder().trim();
                if (!folderPath.endsWith("/")) {
                    folderPath += "/";
                }
            }

            for (String fileName : request.getFileNames()) {
                // Combine folder path with filename
                String objectName = folderPath + fileName;
                String presignedUrl = minioService.generateUploadPresignedUrl(objectName, request.getExpiryHours());

                // Store both the presigned URL and content type info
                presignedUrls.put(fileName, presignedUrl);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("presignedUrls", presignedUrls);
            response.put("folder", request.getFolder());
            response.put("count", presignedUrls.size());
            response.put("expiryHours", request.getExpiryHours());
            response.put("method", "PUT");
            response.put("message", "Upload presigned URLs generated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/presigned-url/download")
    public ResponseEntity<Map<String, Object>> generateDownloadPresignedUrls(
            @RequestBody PresignedUrlRequest request) {

        try {
            Map<String, String> presignedUrls = new HashMap<>();

            // Build folder path - ensure it ends with / if provided
            String folderPath = "";
            if (request.getFolder() != null && !request.getFolder().trim().isEmpty()) {
                folderPath = request.getFolder().trim();
                if (!folderPath.endsWith("/")) {
                    folderPath += "/";
                }
            }

            for (String fileName : request.getFileNames()) {
                // Combine folder path with filename
                String objectName = folderPath + fileName;
                String presignedUrl = minioService.generateDownloadPresignedUrl(objectName, request.getExpiryHours());
                presignedUrls.put(fileName, presignedUrl);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("presignedUrls", presignedUrls);
            response.put("folder", request.getFolder());
            response.put("count", presignedUrls.size());
            response.put("expiryHours", request.getExpiryHours());
            response.put("method", "GET");
            response.put("message", "Download presigned URLs generated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

