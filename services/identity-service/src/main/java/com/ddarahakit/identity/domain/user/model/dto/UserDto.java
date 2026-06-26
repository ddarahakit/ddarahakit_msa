package com.ddarahakit.identity.domain.user.model.dto;

import com.ddarahakit.identity.domain.user.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static com.ddarahakit.identity.common.Constants.DEFAULT_USER_PROFILE_IMAGE_NAME;
import static com.ddarahakit.identity.common.Constants.DEFAULT_USER_ROLE;

public class UserDto {


    @Getter
    @Builder
    public static class UserAccessTokenRes {
        private String accessToken;
        private String refreshToken;

        public static UserAccessTokenRes of(String accessToken, String refreshToken) {
            return UserAccessTokenRes.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }

    @Getter
    public static class UpdatePasswordReq {
        @Schema(description = "현재 비밀번호", example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String originPassword;
        @Schema(description = "새 비밀번호", example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String newPassword1;
        @Schema(description = "새 비밀번호 확인", example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String newPassword2;
    }

    @Getter
    public static class ResetPasswordEmailReq {
        @Schema(description = "이메일", required = true, example = "test01@test.com")
        @Pattern(message = "이메일 양식이 아닙니다.", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private String email;
    }

    @Getter
    public static class ResetPasswordReq {
        @Schema(description = "이메일", required = true, example = "test01@test.com")
        @Pattern(message = "이메일 양식이 아닙니다.", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private String email;
        @Schema(description = "UUID", required = true, example = "c9a78972-fb5a-45d5-ae66-9b90cee97c06")
        private String uuid;
        @Schema(description = "새 비밀번호", required = true, example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String newPassword1;
        @Schema(description = "새 비밀번호 확인", required = true, example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String newPassword2;
    }

    @Getter
    public static class UserProfileReq {
        @Schema(description = "이름", example = "test01")
        @Pattern(message = "닉네임에 특수문자는 사용할 수 없습니다.", regexp = "^[A-Za-z0-9가-힣]{1,50}$")
        private String name;
        @Schema(description = "자기 소개", example = "간단한 소개글")
        private String introduction;
        @Schema(description = "이미지 경로", example = "/default_user.png")
        private String profileImageUrl;
    }

    @Getter
    @Builder
    public static class UserProfileRes {
        private Long idx;
        private String email;
        private String name;
        private String introduction;
        private String profileImageUrl;
        private String provider;

        public static UserProfileRes of(User entity) {
            return UserProfileRes.builder()
                    .idx(entity.getIdx())
                    .email(entity.getEmail())
                    .name(entity.getName())
                    .introduction(entity.getIntroduction())
                    .profileImageUrl(entity.getProfileImageUrl())
                    .provider(entity.getProvider())
                    .build();
        }
    }

    @Getter
    public static class LoginReq {
        @Schema(description = "이메일", required = true, example = "test01@test.com")
        @Pattern(message = "이메일 양식이 아닙니다.", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private String email;
        @Schema(description = "비밀번호", required = true, example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String password;
    }

    @Getter
    @Builder
    public static class LoginRes {
        private Long idx;
        private String email;
        private String name;
        private String profileImageUrl;
        private String role;
        private String provider;

        public static LoginRes of(User user) {
            return LoginRes.builder()
                    .idx(user.getIdx())
                    .email(user.getEmail())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .role(user.getRole())
                    .provider(user.getProvider())
                    .build();
        }
    }

    @Getter
    public static class SignupReq {
        @Schema(description = "이메일", required = true, example = "test01@test.com")
        @Pattern(message = "이메일 양식이 아닙니다.", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private String email;
        @Schema(description = "비밀번호", required = true, example = "Dkagh1234!")
        @Pattern(message = "비밀번호는 숫자,영문,특수문자( !@#$%^&*() )를 조합해 8~20자로 생성해주세요.", regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()])(?=.*[0-9]).{8,15}$")
        private String password;
        @Schema(description = "이름", required = true, example = "test01")
        @Pattern(message = "닉네임에 특수문자는 사용할 수 없습니다.", regexp = "^[A-Za-z0-9가-힣]{1,50}$")
        private String name;

        public User toEntity() {
            return User.builder()
                    .email(email)
                    .name(name)
                    .password(password)
                    .profileImageUrl(DEFAULT_USER_PROFILE_IMAGE_NAME)
                    .role(DEFAULT_USER_ROLE)
                    .provider("email")
                    .enabled(false)
                    .build();
        }
    }

    @Getter
    public static class SignupVerifyReq {
        @Schema(description = "이메일", required = true, example = "test01@test.com")
        @Pattern(message = "이메일 양식이 아닙니다.", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private String email;
        @Schema(description = "UUID", required = true, example = "c9a78972-fb5a-45d5-ae66-9b90cee97c06")
        private String uuid;
    }


    @Getter
    @Builder
    public static class SignupRes {
        private Long idx;
        private String email;
        private String name;
        private String profileImageUrl;
        private String introduction;
        private String phoneNumber;
        private Boolean enabled;
        private String provider;

        public static SignupRes of(User user) {
            return SignupRes.builder()
                    .idx(user.getIdx())
                    .email(user.getEmail())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .introduction(user.getIntroduction())
                    .phoneNumber(user.getPhoneNumber())
                    .enabled(user.getEnabled())
                    .provider(user.getProvider())
                    .build();
        }
    }
}
