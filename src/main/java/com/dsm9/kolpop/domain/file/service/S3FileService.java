package com.dsm9.kolpop.domain.file.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dsm9.kolpop.domain.file.dto.FileUploadResponse;
import com.dsm9.kolpop.global.config.S3Properties;
import com.dsm9.kolpop.global.exception.BusinessException;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3FileService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3FileService(S3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    public FileUploadResponse upload(MultipartFile file) {
        validateConfigured();
        validateFile(file);

        String key = createObjectKey(file.getOriginalFilename());
        String contentType = resolveContentType(file.getContentType());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return new FileUploadResponse(
                    key,
                    resolveFileUrl(key),
                    file.getOriginalFilename(),
                    contentType,
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "업로드할 파일을 읽을 수 없습니다.");
        } catch (SdkException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "S3_UPLOAD_FAILED", "S3 파일 업로드에 실패했습니다.");
        }
    }

    private void validateConfigured() {
        if (!StringUtils.hasText(s3Properties.getBucket())) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "S3_CONFIG_REQUIRED", "S3 버킷 설정이 필요합니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "업로드할 파일이 비어 있습니다.");
        }
    }

    private String createObjectKey(String originalFilename) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String filename = UUID.randomUUID().toString().replace("-", "");

        StringBuilder keyBuilder = new StringBuilder();

        if (StringUtils.hasText(s3Properties.getPrefix())) {
            keyBuilder.append(trimSlashes(s3Properties.getPrefix())).append('/');
        }

        keyBuilder.append(today.getYear())
                .append('/')
                .append(String.format("%02d", today.getMonthValue()))
                .append('/')
                .append(String.format("%02d", today.getDayOfMonth()))
                .append('/')
                .append(filename);

        if (StringUtils.hasText(extension)) {
            keyBuilder.append('.').append(extension.toLowerCase(Locale.ROOT));
        }

        return keyBuilder.toString();
    }

    private String resolveContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }

    private String resolveFileUrl(String key) {
        if (StringUtils.hasText(s3Properties.getPublicBaseUrl())) {
            return trimTrailingSlash(s3Properties.getPublicBaseUrl()) + "/" + key;
        }

        if (StringUtils.hasText(s3Properties.getEndpoint())) {
            return trimTrailingSlash(s3Properties.getEndpoint()) + "/" + s3Properties.getBucket() + "/" + key;
        }

        return "https://" + s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }

    private String trimSlashes(String value) {
        String trimmed = value.strip();

        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value.strip();

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }
}
