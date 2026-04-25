package com.dnd.lambda;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneratePresignedUrlHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String BUCKET_NAME = System.getenv("BUCKET_NAME");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        if (BUCKET_NAME == null || BUCKET_NAME.isEmpty()) {
        throw new RuntimeException("BUCKET_NAME environment variable is not set");
        }

        // Set File Name in S3 to match Ingested PDF Name
        String originalFileName = (String) input.get("fileName");
        
        if (originalFileName == null) {
                throw new RuntimeException("fileName is required");
        }

        String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        String key = "uploads/" + UUID.randomUUID() + "_" + safeFileName;

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1) // change if needed
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType("application/pdf")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        String presignedUrl = presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        Map<String, Object> body = new HashMap<>();
        body.put("uploadUrl", presignedUrl);
        body.put("fileKey", key);

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("headers", Map.of(
                "Access-Control-Allow-Origin", "*"
        ));

        ObjectMapper mapper = new ObjectMapper();

        String jsonBody;

        try {
                jsonBody = mapper.writeValueAsString(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize response", e);
        }

        response.put("body", jsonBody);

        return response;
    }
}
