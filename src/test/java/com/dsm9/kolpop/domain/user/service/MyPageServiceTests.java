package com.dsm9.kolpop.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.user.dto.MyPageResponse;
import com.dsm9.kolpop.domain.user.dto.UpdateMyPageRequest;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

class MyPageServiceTests {

    @Test
    void getMyPageReturnsProfileWithoutAddressFields() {
        UserRepository userRepository = mock(UserRepository.class);
        MyPageService myPageService = new MyPageService(userRepository);
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(1L);

        assertEquals("Landlord", response.name());
        assertEquals("landlord@kolpop.kr", response.email());
        assertEquals("010-1234-5678", response.phone());
        assertEquals("Intro", response.introduction());
        assertEquals("LANDLORD", response.role());
    }

    @Test
    void updateMyPageUpdatesProfileButKeepsAddressInternally() {
        UserRepository userRepository = mock(UserRepository.class);
        MyPageService myPageService = new MyPageService(userRepository);
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("01099998888", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("new@kolpop.kr", 1L)).thenReturn(false);

        MyPageResponse response = myPageService.updateMyPage(1L, new UpdateMyPageRequest(
                "Updated",
                "new@kolpop.kr",
                "010-9999-8888",
                "Updated intro"
        ));

        assertEquals("Updated", response.name());
        assertEquals("new@kolpop.kr", response.email());
        assertEquals("010-9999-8888", response.phone());
        assertEquals("Updated intro", user.getIntroduction());
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
                        "Landlord",
                        "landlord@kolpop.kr",
                        "010-0000-1111",
                        "Intro"
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
                        "Landlord",
                        "taken@kolpop.kr",
                        "010-1234-5678",
                        "Intro"
                ))
        );

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getCode());
    }

    private User createUser() {
        User user = new User(
                "landlord01",
                "Landlord",
                "landlord@kolpop.kr",
                "encodedPassword",
                "01012345678",
                UserRole.LANDLORD
        );
        user.updateProfile("Landlord", "landlord@kolpop.kr", "01012345678", "Intro");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
