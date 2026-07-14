package com.dsm9.kolpop.domain.file.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dsm9.kolpop.domain.file.dto.FileUploadResponse;
import com.dsm9.kolpop.domain.file.service.S3FileService;
import com.dsm9.kolpop.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/files")
@Tag(name = "파일", description = "파일 업로드 API")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final S3FileService s3FileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(s3FileService.upload(file)));
    }
}
