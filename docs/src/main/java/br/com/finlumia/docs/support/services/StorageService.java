package br.com.finlumia.docs.support.services;

import br.com.finlumia.docs.support.config.MinioProperties;
import br.com.finlumia.shared.exception.FinlumiaException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioProperties properties;

    public StorageService(S3Client s3Client, S3Presigner s3Presigner, MinioProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureBucketReady() {
        if (!bucketExists()) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
            log.info("MINIO_BUCKET_CREATED bucket={}", properties.getBucket());
        }
        // CORS do MinIO nao e configuravel por bucket via API (PutBucketCors
        // retorna 501 NotImplemented) — e configurado globalmente no servidor
        // via env var MINIO_API_CORS_ALLOW_ORIGIN (ver docker-compose.dev.yml
        // e finlumia_backend.sh). Lifecycle rule de limpeza de multipart
        // incompleto tambem nao foi aplicada aqui: o MinIO rejeitou o XML
        // gerado pelo SDK (400 schema invalido) — fica como follow-up, nao
        // bloqueia o fluxo principal de upload.
    }

    private boolean bucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    // ---------------------------------------------------------------
    // Object keys
    // ---------------------------------------------------------------

    public String buildRawObjectKey(UUID ticketId, UUID attachmentId, String fileName) {
        return "tickets/%s/%s/original/%s".formatted(ticketId, attachmentId, sanitizeFileName(fileName));
    }

    public String buildConvertedObjectKey(UUID ticketId, UUID attachmentId) {
        return "tickets/%s/%s/converted/video.mp4".formatted(ticketId, attachmentId);
    }

    public String buildThumbnailObjectKey(UUID ticketId, UUID attachmentId) {
        return "tickets/%s/%s/thumbnail.jpg".formatted(ticketId, attachmentId);
    }

    private String sanitizeFileName(String fileName) {
        String base = Path.of(fileName).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ---------------------------------------------------------------
    // Presigned URLs
    // ---------------------------------------------------------------

    public URI presignPutUrl(String objectKey, String contentType, long contentLength, Duration ttl) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                b -> b.signatureDuration(ttl).putObjectRequest(request));
        return toUri(presigned.url());
    }

    public URI presignGetUrl(String objectKey, Duration ttl) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
                b -> b.signatureDuration(ttl).getObjectRequest(request));
        return toUri(presigned.url());
    }

    private URI toUri(java.net.URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("URL assinada invalida: " + url, e);
        }
    }

    // ---------------------------------------------------------------
    // Object access
    // ---------------------------------------------------------------

    public HeadObjectResponse headObject(String objectKey) {
        try {
            return s3Client.headObject(
                    HeadObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build());
        } catch (NoSuchKeyException e) {
            throw new FinlumiaException(404, "Nao encontrado", "Arquivo nao encontrado no storage.");
        }
    }

    public boolean objectExists(String objectKey) {
        try {
            headObject(objectKey);
            return true;
        } catch (FinlumiaException e) {
            return false;
        }
    }

    public void downloadToFile(String objectKey, Path target) {
        s3Client.getObject(
                GetObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build(), target);
    }

    public void uploadFile(String objectKey, Path sourceFile, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(sourceFile));
    }
}
