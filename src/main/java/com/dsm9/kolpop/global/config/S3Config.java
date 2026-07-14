package com.dsm9.kolpop.global.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(S3Properties s3Properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .forcePathStyle(s3Properties.isPathStyleAccessEnabled());

        if (hasStaticCredentials(s3Properties)) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                    )
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        }

        if (StringUtils.hasText(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        return builder.build();
    }

    private boolean hasStaticCredentials(S3Properties s3Properties) {
        return StringUtils.hasText(s3Properties.getAccessKey())
                && StringUtils.hasText(s3Properties.getSecretKey());
    }
}
