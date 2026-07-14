package com.dsm9.kolpop.domain.file.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.dsm9.kolpop.domain.file.dto.FileUploadResponse;
import com.dsm9.kolpop.global.config.S3Properties;
import com.dsm9.kolpop.global.exception.BusinessException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3FileServiceTests {

    @Test
    void uploadStoresFileUsingConfiguredBucketAndPrefix() {
        S3Client s3Client = mock(S3Client.class);
        S3FileService s3FileService = new S3FileService(s3Client, createProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.PNG",
                "image/png",
                "kolpop".getBytes()
        );

        FileUploadResponse response = s3FileService.upload(file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("kolpop-bucket", request.bucket());
        assertEquals("image/png", request.contentType());
        assertTrue(request.key().startsWith("uploads/"));
        assertTrue(request.key().endsWith(".png"));
        assertEquals(request.key(), response.key());
        assertEquals("https://cdn.kolpop.test/" + request.key(), response.url());
        assertEquals("profile.PNG", response.originalFilename());
        assertEquals("image/png", response.contentType());
        assertEquals(file.getSize(), response.size());
    }

    @Test
    void emptyFileIsRejected() {
        S3Client s3Client = mock(S3Client.class);
        S3FileService s3FileService = new S3FileService(s3Client, createProperties());
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        BusinessException exception = assertThrows(BusinessException.class, () -> s3FileService.upload(file));

        assertEquals("EMPTY_FILE", exception.getCode());
    }

    private S3Properties createProperties() {
        S3Properties properties = new S3Properties();
        properties.setRegion("ap-northeast-2");
        properties.setBucket("kolpop-bucket");
        properties.setPublicBaseUrl("https://cdn.kolpop.test");
        properties.setPrefix("uploads");
        return properties;
    }
}
