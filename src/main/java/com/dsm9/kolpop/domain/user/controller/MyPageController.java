package com.dsm9.kolpop.domain.user.controller;

import com.dsm9.kolpop.domain.user.dto.MyPageResponse;
import com.dsm9.kolpop.domain.user.dto.UpdateMyPageRequest;
import com.dsm9.kolpop.domain.user.service.MyPageService;
import com.dsm9.kolpop.global.exception.BusinessException;
import com.dsm9.kolpop.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    @GetMapping
    public ApiResponse<MyPageResponse> getMyPage(Authentication authentication) {
        return ApiResponse.success(myPageService.getMyPage(extractUserId(authentication)));
    }

    @PutMapping
    public ApiResponse<MyPageResponse> updateMyPage(
            Authentication authentication,
            @Valid @RequestBody UpdateMyPageRequest request
    ) {
        return ApiResponse.success(myPageService.updateMyPage(extractUserId(authentication), request));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다.");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION", "인증 정보가 올바르지 않습니다.");
        }
    }
}
