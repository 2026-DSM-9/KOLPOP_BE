package com.dsm9.kolpop.auth.repository;

import com.dsm9.kolpop.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhone(String phone);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);
}
