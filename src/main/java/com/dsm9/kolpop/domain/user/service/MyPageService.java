package com.dsm9.kolpop.domain.user.service;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.user.dto.MyPageResponse;
import com.dsm9.kolpop.domain.user.dto.UpdateMyPageRequest;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyPageService {

    private final UserRepository userRepository;

    public MyPageService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId) {
        return toResponse(getUser(userId));
    }

    @Transactional
    public MyPageResponse updateMyPage(Long userId, UpdateMyPageRequest request) {
        User user = getUser(userId);
        String normalizedPhone = normalizePhone(request.phone());
        String normalizedEmail = normalizeEmail(request.email());

        validateUniquePhone(user.getId(), normalizedPhone);
        validateUniqueEmail(user.getId(), normalizedEmail);

        user.updateProfile(
                request.name().trim(),
                normalizedEmail,
                normalizedPhone,
                normalizeNullable(request.address()),
                normalizeNullable(request.detailAddress()),
                normalizeNullable(request.introduction())
        );

        return toResponse(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    private void validateUniquePhone(Long userId, String phone) {
        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS", "이미 가입된 전화번호입니다.");
        }
    }

    private void validateUniqueEmail(Long userId, String email) {
        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
        }
    }

    private MyPageResponse toResponse(User user) {
        return new MyPageResponse(
                user.getName(),
                nullToEmpty(user.getEmail()),
                formatPhone(user.getPhone()),
                nullToEmpty(user.getAddress()),
                nullToEmpty(user.getDetailAddress()),
                nullToEmpty(user.getIntroduction()),
                user.getRole().name()
        );
    }

    private String normalizePhone(String phone) {
        return phone.replace("-", "").trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String formatPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
