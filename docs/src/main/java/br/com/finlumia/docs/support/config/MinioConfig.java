package br.com.finlumia.docs.support.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public S3Client minioS3Client(MinioProperties properties) {
        StaticCredentialsProvider credentials = credentials(properties);
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getInternalEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // SDK v2.25+ passou a enviar checksum por padrao em mais operacoes
                // (WHEN_SUPPORTED); o MinIO responde 501 pra algumas delas (ex:
                // PutBucketCors). WHEN_REQUIRED volta ao comportamento classico.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner minioS3Presigner(MinioProperties properties) {
        StaticCredentialsProvider credentials = credentials(properties);
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getPublicEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private StaticCredentialsProvider credentials(MinioProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }
}
