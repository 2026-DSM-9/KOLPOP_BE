package com.dsm9.kolpop.domain.auth.service;

import com.dsm9.kolpop.domain.auth.dto.FounderLoginResponse;
import com.dsm9.kolpop.domain.auth.dto.LoginRequest;
import com.dsm9.kolpop.domain.auth.dto.LoginResponse;
import com.dsm9.kolpop.domain.auth.dto.LoginUserResponse;
import com.dsm9.kolpop.domain.auth.dto.LoginIdCheckRequest;
import com.dsm9.kolpop.domain.auth.dto.ReissueResponse;
import com.dsm9.kolpop.domain.auth.dto.SignupCodeSendRequest;
import com.dsm9.kolpop.domain.auth.dto.SignupRequest;
import com.dsm9.kolpop.domain.auth.dto.SignupResponse;
import com.dsm9.kolpop.domain.auth.dto.SignupUserResponse;
import com.dsm9.kolpop.domain.auth.dto.SignupVerifyRequest;
import com.dsm9.kolpop.domain.auth.jwt.JwtAuthenticationFilter;
import com.dsm9.kolpop.domain.auth.jwt.JwtTokenProvider;
import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.global.exception.BusinessException;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration SIGNUP_VERIFICATION_TTL = Duration.ofMinutes(5);
    private static final Duration SIGNUP_VERIFIED_TTL = Duration.ofMinutes(10);
    private static final String SIGNUP_VERIFICATION_KEY_PREFIX = "signup:verification:";
    private static final String SIGNUP_VERIFIED_KEY_PREFIX = "signup:verified:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh-token:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final String coolSmsApiKey;
    private final String coolSmsApiSecret;
    private final String coolSmsSender;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate stringRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            @Value("${coolsms.api-key}") String coolSmsApiKey,
            @Value("${coolsms.api-secret}") String coolSmsApiSecret,
            @Value("${coolsms.sender}") String coolSmsSender
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.coolSmsApiKey = coolSmsApiKey;
        this.coolSmsApiSecret = coolSmsApiSecret;
        this.coolSmsSender = normalizePhone(coolSmsSender);
    }

    public void verifySignup(SignupVerifyRequest request) {
        String phone = normalizePhone(request.phone());
        String verificationKey = SIGNUP_VERIFICATION_KEY_PREFIX + phone;
        String savedCode = stringRedisTemplate.opsForValue().get(verificationKey);

        if (savedCode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_EXPIRED", "인증번호가 만료되었거나 전송되지 않았습니다.");
        }

        if (!savedCode.equals(request.code())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "인증번호가 올바르지 않습니다.");
        }

        stringRedisTemplate.delete(verificationKey);
        stringRedisTemplate.opsForValue().set(
                SIGNUP_VERIFIED_KEY_PREFIX + phone,
                "verified",
                SIGNUP_VERIFIED_TTL
        );
    }

    public void checkLoginId(LoginIdCheckRequest request) {
        validateLoginIdAvailable(request.loginId());
    }

    public void sendSignupCode(SignupCodeSendRequest request) {
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS", "이미 가입된 전화번호입니다.");
        }

        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        stringRedisTemplate.delete(SIGNUP_VERIFIED_KEY_PREFIX + phone);
        stringRedisTemplate.opsForValue().set(
                SIGNUP_VERIFICATION_KEY_PREFIX + phone,
                code,
                SIGNUP_VERIFICATION_TTL
        );
        sendMessage(phone, "[KOLPOP] 회원가입 인증번호는 " + code + "입니다.");
    }

    public SignupResponse signupLandlord(SignupRequest request) {
        return signup(request, UserRole.LANDLORD);
    }

    public SignupResponse signupFounder(SignupRequest request) {
        return signup(request, UserRole.FOUNDER);
    }

    public FounderLoginResponse loginFounder(LoginRequest request) {
        LoginTokens loginTokens = login(request, UserRole.FOUNDER);

        return new FounderLoginResponse(
                loginTokens.accessToken(),
                loginTokens.refreshToken(),
                jwtTokenProvider.getAccessTokenExpiresIn(),
                loginTokens.user()
        );
    }

    public LandlordLoginResult loginLandlord(LoginRequest request) {
        LoginTokens loginTokens = login(request, UserRole.LANDLORD);

        return new LandlordLoginResult(
                new LoginResponse(
                        loginTokens.accessToken(),
                        jwtTokenProvider.getAccessTokenExpiresIn(),
                        loginTokens.user()
                ),
                loginTokens.refreshToken()
        );
    }

    public ReissueResponse reissue(String authorizationHeader) {
        String refreshToken = extractBearerToken(authorizationHeader);
        if (!jwtTokenProvider.isValidRefreshToken(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다.");
        }

        Long userId = Long.valueOf(jwtTokenProvider.getUserId(refreshToken));
        String savedRefreshToken = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + userId);
        if (!refreshToken.equals(savedRefreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        saveRefreshToken(user.getId(), newRefreshToken);

        return new ReissueResponse(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiresIn(),
                jwtTokenProvider.getRefreshTokenExpiresIn()
        );
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (!jwtTokenProvider.isValidAccessToken(token)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }

        Long userId = Long.valueOf(jwtTokenProvider.getUserId(token));
        stringRedisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + userId);

        long remainingSeconds = jwtTokenProvider.getRemainingSeconds(token);
        stringRedisTemplate.opsForValue().set(
                JwtAuthenticationFilter.BLACKLIST_KEY_PREFIX + token,
                "logout",
                Duration.ofSeconds(remainingSeconds)
        );
    }

    public long getRefreshTokenExpiresIn() {
        return jwtTokenProvider.getRefreshTokenExpiresIn();
    }

    private SignupResponse signup(SignupRequest request, UserRole role) {
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS", "이미 가입된 전화번호입니다.");
        }

        String verifiedKey = SIGNUP_VERIFIED_KEY_PREFIX + phone;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(verifiedKey))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PHONE_VERIFICATION_REQUIRED", "전화번호 인증이 필요합니다.");
        }

        validateLoginIdAvailable(request.loginId());

        if (request.email() != null && !request.email().isBlank() && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_MISMATCH", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        User user = new User(
                request.loginId(),
                request.name(),
                normalizeEmail(request.email()),
                passwordEncoder.encode(request.password()),
                phone,
                role
        );

        User savedUser = userRepository.saveAndFlush(user);
        String accessToken = jwtTokenProvider.createAccessToken(savedUser);
        stringRedisTemplate.delete(verifiedKey);

        return new SignupResponse(
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresIn(),
                toSignupUserResponse(savedUser)
        );
    }

    private LoginTokens login(LoginRequest request, UserRole requestedRole) {
        User user = userRepository.findByLoginId(request.loginId().trim())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        saveRefreshToken(user.getId(), refreshToken);

        return new LoginTokens(accessToken, refreshToken, toLoginUserResponse(user, requestedRole));
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY_PREFIX + userId,
                refreshToken,
                Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpiresIn())
        );
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_REQUIRED", "Bearer Token이 필요합니다.");
        }
        return authorizationHeader.substring(7);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email;
    }

    private void validateLoginIdAvailable(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "LOGIN_ID_ALREADY_EXISTS", "이미 사용 중인 아이디입니다.");
        }
    }

    private String normalizePhone(String phone) {
        return phone.replace("-", "");
    }

    private SignupUserResponse toSignupUserResponse(User user) {
        return new SignupUserResponse(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getRole().name()
        );
    }

    private LoginUserResponse toLoginUserResponse(User user, UserRole role) {
        return new LoginUserResponse(
                String.valueOf(user.getId()),
                role == UserRole.LANDLORD ? formatPhone(user.getPhone()) : user.getPhone(),
                user.getName()
        );
    }

    private String formatPhone(String phone) {
        if (phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
    }

    private void sendMessage(String phone, String text) {
        if (coolSmsApiKey.isBlank() || coolSmsApiSecret.isBlank() || coolSmsSender.isBlank()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "SMS_CONFIG_REQUIRED", "문자 발송 설정이 필요합니다.");
        }

        try {
            DefaultMessageService messageService = SolapiClient.INSTANCE.createInstance(coolSmsApiKey, coolSmsApiSecret);
            Message message = new Message();
            message.setFrom(coolSmsSender);
            message.setTo(phone);
            message.setText(text);

            messageService.send(message, null);
        } catch (Exception exception) {
            log.warn("Failed to send signup verification message to {}", phone, exception);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "SMS_SEND_FAILED", "인증번호 문자 발송에 실패했습니다.");
        }
    }

    private record LoginTokens(
            String accessToken,
            String refreshToken,
            LoginUserResponse user
    ) {
    }

    public record LandlordLoginResult(
            LoginResponse response,
            String refreshToken
    ) {
    }
}
