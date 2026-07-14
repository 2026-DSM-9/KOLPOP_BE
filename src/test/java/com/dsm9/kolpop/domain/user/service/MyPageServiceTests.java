package com.dsm9.kolpop.domain.user.service;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.user.dto.MyPageResponse;
import com.dsm9.kolpop.domain.user.dto.UpdateMyPageRequest;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyPageServiceTests {

    @Test
    void getMyPageReturnsFormattedProfile() {
        UserRepository userRepository = mock(UserRepository.class);
        MyPageService myPageService = new MyPageService(userRepository);
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(1L);

        assertEquals("김임대", response.name());
        assertEquals("landlord@kolpop.kr", response.email());
        assertEquals("010-1234-5678", response.phone());
        assertEquals("서울 강남구 테헤란로 123", response.address());
        assertEquals("101동 202호", response.detailAddress());
    }

    @Test
    void updateMyPageUpdatesProfile() {
        UserRepository userRepository = mock(UserRepository.class);
        MyPageService myPageService = new MyPageService(userRepository);
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("01099998888", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("new@kolpop.kr", 1L)).thenReturn(false);

        MyPageResponse response = myPageService.updateMyPage(1L, new UpdateMyPageRequest(
                "김수정",
                "new@kolpop.kr",
                "010-9999-8888",
                "서울 마포구 양화로 153",
                "2층",
                "팝업 공간 운영 경험이 많습니다."
        ));

        assertEquals("김수정", response.name());
        assertEquals("new@kolpop.kr", response.email());
        assertEquals("010-9999-8888", response.phone());
        assertEquals("서울 마포구 양화로 153", user.getAddress());
        assertEquals("2층", user.getDetailAddress());
    }

    @Test
    void duplicatePhoneIsRejected() {
        UserRepository userRepository = mock(UserRepository.class);
        MyPageService myPageService = new MyPageService(userRepository);
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("01000001111", 1L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> myPageService.updateMyPage(1L, new UpdateMyPageRequest(
                        "김임대",
                        "landlord@kolpop.kr",
                        "010-0000-1111",
                        "서울 강남구 테헤란로 123",
                        "101동 202호",
                        "소개"
                ))
        );

        assertEquals("PHONE_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void duplicateEmailIsRejected() {
        UserRepository userRepository = mock(UserRepository.class);
        MyPageService myPageService = new MyPageService(userRepository);
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("01012345678", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("taken@kolpop.kr", 1L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> myPageService.updateMyPage(1L, new UpdateMyPageRequest(
                        "김임대",
                        "taken@kolpop.kr",
                        "010-1234-5678",
                        "서울 강남구 테헤란로 123",
                        "101동 202호",
                        "소개"
                ))
        );

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getCode());
    }

    private User createUser() {
        User user = new User(
                "landlord01",
                "김임대",
                "landlord@kolpop.kr",
                "encodedPassword",
                "01012345678",
                "서울 강남구 테헤란로 123",
                "101동 202호",
                "홍대와 강남권 중심으로 단기 팝업 공간을 직접 운영하고 있습니다.",
                UserRole.LANDLORD
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
