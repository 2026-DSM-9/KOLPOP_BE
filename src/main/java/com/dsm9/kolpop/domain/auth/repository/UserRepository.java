package com.dsm9.kolpop.domain.auth.repository;

import com.dsm9.kolpop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByPhone(String phone);

    Optional<User> findByLoginId(String loginId);
}
