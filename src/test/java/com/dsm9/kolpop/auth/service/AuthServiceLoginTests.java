package com.dsm9.kolpop.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.auth.dto.LoginRequest;
import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.auth.jwt.JwtTokenProvider;
import com.dsm9.kolpop.domain.auth.service.AuthService;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;

class AuthServiceLoginTests {

    @Test
    void landlordLoginUsesLoginId() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                stringRedisTemplate,
                jwtTokenProvider,
                "test-api-key",
                "test-api-secret",
                "01012345678"
        );
        User user = createUser(1L, UserRole.LANDLORD);

        when(userRepository.findByLoginId("landlord01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(user)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiresIn()).thenReturn(3600L);
        when(jwtTokenProvider.getRefreshTokenExpiresIn()).thenReturn(604800L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthService.LandlordLoginResult result = authService.loginLandlord(new LoginRequest("landlord01", "password"));

        assertEquals("access-token", result.response().accessToken());
        assertEquals("landlord01", user.getLoginId());
        verify(userRepository).findByLoginId("landlord01");
    }

    private User createUser(Long id, UserRole role) {
        User user = new User(
                "landlord01",
                "Landlord",
                "landlord@kolpop.kr",
                "encodedPassword",
                "01012345678",
                role
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
