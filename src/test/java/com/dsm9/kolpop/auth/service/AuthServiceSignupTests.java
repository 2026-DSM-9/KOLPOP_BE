package com.dsm9.kolpop.auth.service;

import com.dsm9.kolpop.domain.auth.dto.SignupRequest;
import com.dsm9.kolpop.domain.auth.dto.SignupResponse;
import com.dsm9.kolpop.domain.auth.dto.SignupVerifyRequest;
import com.dsm9.kolpop.domain.auth.dto.LoginIdCheckRequest;
import com.dsm9.kolpop.domain.auth.service.AuthService;
import com.dsm9.kolpop.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Transactional
@SpringBootTest
class AuthServiceSignupTests {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
    }

    @Test
    void landlordSignupReturnsLandlordRoleAndAccessToken() {
        SignupRequest request = createRequest("landlord");
        SignupResponse response = authService.signupLandlord(request);

        assertEquals("LANDLORD", response.user().role());
        assertFalse(response.accessToken().isBlank());
        assertTrue(response.expiresIn() > 0);
    }

    @Test
    void founderSignupReturnsFounderRoleAndAccessToken() {
        SignupRequest request = createRequest("founder");
        SignupResponse response = authService.signupFounder(request);

        assertEquals("FOUNDER", response.user().role());
        assertFalse(response.accessToken().isBlank());
        assertTrue(response.expiresIn() > 0);
    }

    @Test
    void verificationCodeMarksPhoneAsVerified() {
        when(valueOperations.get("signup:verification:01011112222")).thenReturn("123456");

        authService.verifySignup(new SignupVerifyRequest("010-1111-2222", "123456"));

        verify(stringRedisTemplate).delete("signup:verification:01011112222");
        verify(valueOperations).set(
                "signup:verified:01011112222",
                "verified",
                Duration.ofMinutes(10)
        );
    }

    @Test
    void wrongVerificationCodeIsRejected() {
        when(valueOperations.get("signup:verification:01011112222")).thenReturn("654321");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.verifySignup(new SignupVerifyRequest("010-1111-2222", "123456"))
        );

        assertEquals("INVALID_VERIFICATION_CODE", exception.getCode());
    }

    @Test
    void signupWithoutPhoneVerificationIsRejected() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.signupLandlord(createRequest("unverified"))
        );

        assertEquals("PHONE_VERIFICATION_REQUIRED", exception.getCode());
    }

    @Test
    void availableLoginIdPassesDuplicateCheck() {
        String loginId = "available_" + UUID.randomUUID().toString().substring(0, 8);

        authService.checkLoginId(new LoginIdCheckRequest(loginId));
    }

    @Test
    void duplicateLoginIdIsRejected() {
        SignupRequest request = createRequest("duplicate");
        authService.signupLandlord(request);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.checkLoginId(new LoginIdCheckRequest(request.loginId()))
        );

        assertEquals("LOGIN_ID_ALREADY_EXISTS", exception.getCode());
    }

    private SignupRequest createRequest(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String phoneSuffix = String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));

        return new SignupRequest(
                prefix + "_" + suffix,
                "가입 테스트",
                "password123!",
                "password123!",
                "010" + phoneSuffix,
                null
        );
    }

}
