package com.ddarahakit.identity.domain.user.service;

import com.ddarahakit.identity.common.exception.BaseException;
import com.ddarahakit.identity.config.security.AuthUserDetails;
import com.ddarahakit.identity.domain.image.FileUploadService;
import com.ddarahakit.identity.domain.user.model.dto.UserDto;
import com.ddarahakit.identity.domain.user.model.entity.EmailVerify;
import com.ddarahakit.identity.domain.user.model.entity.User;
import com.ddarahakit.identity.domain.user.repository.EmailVerifyRepository;
import com.ddarahakit.identity.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ddarahakit.identity.common.Constants.*;
import static com.ddarahakit.identity.common.model.BaseResponseStatus.*;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailVerifyRepository emailVerifyRepository;
    private final FileUploadService fileUploadService;

    /**
     * 로그인
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return AuthUserDetails.of(user);
    }

    /**
     * 회원 가입
     */
    @Transactional
    public UserDto.SignupRes signup(UserDto.SignupReq dto) {
        /// 이메일 중복 확인
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw BaseException.of(DUPLICATE_USER_EMAIL);
        }

        /// 회원 정보 DB에 저장
        User user = dto.toEntity();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user = userRepository.save(user);

        /// 이메일 인증 메일 전송
        String uuid = UUID.randomUUID().toString();
        emailService.sendEmail(user.getEmail(), uuid, EMAIL_TYPE_SIGNUP);

        /// 이메일 인증 메일 정보 DB에 저장
        EmailVerify emailVerify = EmailVerify.builder()
                .user(user)
                .uuid(uuid)
                .type(EMAIL_TYPE_SIGNUP)
                .build();
        emailVerifyRepository.save(emailVerify);

        return UserDto.SignupRes.of(user);
    }

    /**
     * 회원 가입 후 이메일 인증
     */
    @Transactional
    public UserDto.SignupRes signupVerify(UserDto.SignupVerifyReq dto) {
        EmailVerify emailVerify = emailVerifyRepository.findByUuidAndType(dto.getUuid(), EMAIL_TYPE_SIGNUP).orElseThrow(
                () -> BaseException.of(EMAIL_VERIFY_NOT_FOUND)
        );

        User user = emailVerify.getUser();
        if (!user.getEmail().equals(dto.getEmail())) {
            throw BaseException.of(INVALID_USER_EMAIL);
        }

        user.setEnabled(true);
        userRepository.save(user);


        emailVerifyRepository.delete(emailVerify);

        return UserDto.SignupRes.of(user);
    }

    /**
     * 프로필
     */
    public UserDto.UserProfileRes getUserProfile(AuthUserDetails authUserDetails) {
        User user = userRepository.findById(authUserDetails.getIdx()).orElseThrow(
                () -> BaseException.of(USER_NOT_FOUND)
        );

        return UserDto.UserProfileRes.of(user);
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserDto.UserProfileRes updateUserProfile(AuthUserDetails authUserDetails, UserDto.UserProfileReq dto) {
        User user = userRepository.findById(authUserDetails.getIdx()).orElseThrow(
                () -> BaseException.of(USER_NOT_FOUND)
        );

        user.updateProfile(dto);
        userRepository.save(user);

        return UserDto.UserProfileRes.of(user);
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserDto.UserProfileRes updateUserProfile(AuthUserDetails authUserDetails, MultipartFile multipartFile) {
        User user = userRepository.findById(authUserDetails.getIdx()).orElseThrow(
                () -> BaseException.of(USER_NOT_FOUND)
        );

        String savedFileName = fileUploadService.uploadProfile(multipartFile);

        user.updateProfileImage(savedFileName);
        userRepository.save(user);

        return UserDto.UserProfileRes.of(user);
    }

    /**
     * 비밀번호 찾기 이메일 요청
     */
    @Transactional
    public void resetPasswordEmailReq(UserDto.ResetPasswordEmailReq dto) {
        // 사용자 열거(enumeration) 방지: 미가입 이메일도 동일하게 성공 응답을 반환하고,
        // 실제 가입된 경우에만 메일 발송/토큰 저장을 수행한다.
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        String uuid = UUID.randomUUID().toString();
        emailService.sendEmail(dto.getEmail(), uuid, EMAIL_TYPE_PASSWORD_RESET);
        EmailVerify emailVerify = EmailVerify.builder()
                .user(user)
                .uuid(uuid)
                .type(EMAIL_TYPE_PASSWORD_RESET)
                .build();
        emailVerifyRepository.save(emailVerify);
    }

    /**
     * 비밀번호 찾기 후 비밀번호 변경
     */
    @Transactional
    public UserDto.SignupRes resetPassword(UserDto.ResetPasswordReq dto) {
        EmailVerify emailVerify = emailVerifyRepository.findByUuidAndType(dto.getUuid(), EMAIL_TYPE_PASSWORD_RESET).orElseThrow(
                () -> BaseException.of(EMAIL_VERIFY_NOT_FOUND)
        );

        User user = emailVerify.getUser();
        if (!user.getEmail().equals(dto.getEmail())) {
            throw BaseException.of(INVALID_USER_EMAIL);
        }

        Date createdAt = emailVerify.getCreatedAt();
        Date now = new Date();

        if (now.getTime() - createdAt.getTime() > EMAIL_RESET_TIMEOUT) {
            throw BaseException.of(INVALID_EMAIL_RESET_TIMEOUT);
        }

        if (!dto.getNewPassword1().equals(dto.getNewPassword2())) {
            throw BaseException.of(INVALID_USER_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword1()));
        User updatedUser = userRepository.save(user);

        emailVerifyRepository.delete(emailVerify);

        return UserDto.SignupRes.of(updatedUser);
    }

    /**
     * 로그인 한 상태에서 비밀번호 변경
     */
    @Transactional
    public UserDto.SignupRes updatePassword(AuthUserDetails authUserDetails, UserDto.UpdatePasswordReq dto) {
        User user = userRepository.findById(authUserDetails.getIdx()).orElseThrow(
                () -> BaseException.of(USER_NOT_FOUND)
        );

        if (!passwordEncoder.matches(dto.getOriginPassword(), user.getPassword()) ||
                !dto.getNewPassword1().equals(dto.getNewPassword2())
        ) {
            throw BaseException.of(INVALID_USER_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword1()));
        User updatedUser = userRepository.save(user);

        return UserDto.SignupRes.of(updatedUser);
    }

    /**
     * UUID 만료 시간 확인
     */
    public boolean checkUuidExpired(String email, String uuid) {
        EmailVerify emailVerify = emailVerifyRepository.findByUuid(uuid).orElseThrow(
                () -> BaseException.of(EMAIL_VERIFY_NOT_FOUND)
        );

        if (!emailVerify.getUser().getEmail().equals(email)) {
            throw BaseException.of(INVALID_USER_EMAIL);
        }

        Date createdAt = emailVerify.getCreatedAt();
        Date now = new Date();

        if (now.getTime() - createdAt.getTime() > EMAIL_RESET_TIMEOUT) {
            throw BaseException.of(INVALID_EMAIL_RESET_TIMEOUT);
        }

        return true;
    }
}
