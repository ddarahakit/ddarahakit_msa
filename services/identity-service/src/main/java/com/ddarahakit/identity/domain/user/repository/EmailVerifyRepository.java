package com.ddarahakit.identity.domain.user.repository;


import com.ddarahakit.identity.domain.user.model.entity.EmailVerify;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {

    Optional<EmailVerify> findByUuidAndType(String uuid, String type);

    Optional<EmailVerify> findByUuid(String uuid);
}
