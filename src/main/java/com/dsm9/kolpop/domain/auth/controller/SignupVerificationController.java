package com.dsm9.kolpop.domain.auth.controller;

import com.dsm9.kolpop.domain.auth.dto.SignupCodeSendRequest;
import com.dsm9.kolpop.domain.auth.dto.SignupVerifyRequest;
import com.dsm9.kolpop.domain.auth.service.AuthService;
import com.dsm9.kolpop.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/signup")
public class SignupVerificationController {

    private final AuthService authService;

    @PostMapping("/send")
    public ApiResponse<Void> sendSignupCode(@Valid @RequestBody SignupCodeSendRequest request) {
        authService.sendSignupCode(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verifySignupCode(@Valid @RequestBody SignupVerifyRequest request) {
        authService.verifySignup(request);
        return ApiResponse.success(null);
    }
}
