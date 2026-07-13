package com.dsm9.kolpop.auth.controller;

import com.dsm9.kolpop.auth.dto.FounderLoginResponse;
import com.dsm9.kolpop.auth.dto.LoginRequest;
import com.dsm9.kolpop.auth.dto.LoginResponse;
import com.dsm9.kolpop.auth.dto.LoginIdCheckRequest;
import com.dsm9.kolpop.auth.dto.ReissueResponse;
import com.dsm9.kolpop.auth.dto.SignupCodeSendRequest;
import com.dsm9.kolpop.auth.dto.SignupRequest;
import com.dsm9.kolpop.auth.dto.SignupResponse;
import com.dsm9.kolpop.auth.dto.SignupVerifyRequest;
import com.dsm9.kolpop.auth.service.AuthService;
import com.dsm9.kolpop.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/check-id")
    public ApiResponse<Void> checkLoginId(@Valid @RequestBody LoginIdCheckRequest request) {
        authService.checkLoginId(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verifySignup(@Valid @RequestBody SignupVerifyRequest request) {
        authService.verifySignup(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/send")
    public ApiResponse<Void> sendSignupCode(@Valid @RequestBody SignupCodeSendRequest request) {
        authService.sendSignupCode(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/landlord/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signupLandlord(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.signupLandlord(request)));
    }

    @PostMapping("/entrepreneur/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signupFounder(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.signupFounder(request)));
    }

    @PostMapping("/entrepreneur/login")
    public ApiResponse<FounderLoginResponse> loginFounder(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.loginFounder(request));
    }

    @PostMapping("/landlord/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginLandlord(@Valid @RequestBody LoginRequest request) {
        AuthService.LandlordLoginResult loginResult = authService.loginLandlord(request);
        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(loginResult.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(loginResult.response()));
    }

    @PostMapping("/reissue")
    public ApiResponse<ReissueResponse> reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ApiResponse.success(authService.reissue(authorizationHeader));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ApiResponse.success(null);
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(authService.getRefreshTokenExpiresIn())
                .sameSite("Lax")
                .build();
    }
}
