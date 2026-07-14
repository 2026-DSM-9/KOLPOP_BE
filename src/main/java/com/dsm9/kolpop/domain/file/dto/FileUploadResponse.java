package com.dsm9.kolpop.domain.file.dto;

public record FileUploadResponse(
        String key,
        String url,
        String originalFilename,
        String contentType,
        long size
) {
}
