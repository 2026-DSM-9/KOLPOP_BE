package com.dsm9.kolpop.domain.auth.jwt;

import com.dsm9.kolpop.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtTokenProvider {

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\"sub\":\"([^\"]+)\"");
    private static final Pattern TOKEN_TYPE_PATTERN = Pattern.compile("\"tokenType\":\"([^\"]+)\"");
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile("\"exp\":(\\d+)");
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final String secret;
    private final long accessTokenExpiresIn;
    private final long refreshTokenExpiresIn;

    public JwtTokenProvider(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.access-token-expires-in}") long accessTokenExpiresIn,
            @Value("${auth.jwt.refresh-token-expires-in}") long refreshTokenExpiresIn
    ) {
        this.secret = secret;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    public String createAccessToken(User user) {
        return createToken(user, ACCESS_TOKEN_TYPE, accessTokenExpiresIn);
    }

    public String createRefreshToken(User user) {
        return createToken(user, REFRESH_TOKEN_TYPE, refreshTokenExpiresIn);
    }

    public boolean isValidAccessToken(String token) {
        return isValidToken(token) && ACCESS_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isValidRefreshToken(String token) {
        return isValidToken(token) && REFRESH_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isValidToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            return sign(unsignedToken).equals(parts[2]) && getRemainingSeconds(token) > 0;
        } catch (Exception exception) {
            return false;
        }
    }

    public String getUserId(String token) {
        return findPayloadValue(token, SUBJECT_PATTERN);
    }

    public long getRemainingSeconds(String token) {
        long expiresAt = Long.parseLong(findPayloadValue(token, EXPIRATION_PATTERN));
        return expiresAt - Instant.now().getEpochSecond();
    }

    public long getAccessTokenExpiresIn() {
        return accessTokenExpiresIn;
    }

    public long getRefreshTokenExpiresIn() {
        return refreshTokenExpiresIn;
    }

    private String createToken(User user, String tokenType, long expiresIn) {
        Instant now = Instant.now();
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"sub\":\"%s\",\"phone\":\"%s\",\"tokenType\":\"%s\",\"iat\":%d,\"exp\":%d}",
                user.getId(),
                user.getPhone(),
                tokenType,
                now.getEpochSecond(),
                now.plusSeconds(expiresIn).getEpochSecond()
        );

        String encodedHeader = encode(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = encodedHeader + "." + encodedPayload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private String getTokenType(String token) {
        return findPayloadValue(token, TOKEN_TYPE_PATTERN);
    }

    private String findPayloadValue(String token, Pattern pattern) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("잘못된 JWT 토큰입니다.");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            throw new IllegalArgumentException("JWT 토큰 정보를 찾을 수 없습니다.");
        }
        return matcher.group(1);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 토큰 생성에 실패했습니다.", exception);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
