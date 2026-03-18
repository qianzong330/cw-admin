package com.example.hello.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Sealos对象存储S3配置
 */
@Configuration
public class SealosS3Config {

    @Value("${sealos.s3.endpoint:http://minio.objectstorage-system.svc.cluster.local}")
    private String endpoint;

    @Value("${sealos.s3.access-key:z4bn2xr7}")
    private String accessKey;

    @Value("${sealos.s3.secret-key:4v5csvjh4mcfs2dw}")
    private String secretKey;

    @Value("${sealos.s3.bucket:z4bn2xr7-hsc-images}")
    private String bucketName;

    @Value("${sealos.s3.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)  // 虚拟主机风格，兼容阿里云OSS
                        .build())
                .build();
    }

    @Bean
    public String bucketName() {
        return bucketName;
    }

    @Bean
    public String s3Endpoint() {
        return endpoint;
    }
}
