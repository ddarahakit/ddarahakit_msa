package com.ddarahakit.identity.domain.user.model.entity;

import com.ddarahakit.identity.common.model.BaseEntity;
import com.ddarahakit.identity.domain.user.model.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDX", nullable = false)
    private Long idx;

    @Column(name = "EMAIL", nullable = false, length = 50, unique = true)
    @Schema(description = "이메일")
    private String email;

    @Setter
    @Column(name = "PASSWORD", nullable = false, length = 200)
    @Schema(description = "비밀번호")
    private String password;

    @Column(name = "NAME", nullable = false, length = 20)
    @Schema(description = "이름")
    private String name;

    @Column(name = "PROFILE_IMAGE_URL", length = 200)
    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Column(name = "INTRODUCTION", length = 200)
    @Schema(description = "자기 소개")
    private String introduction;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "잘못된 번호입니다.")
    @Column(name = "PHONE_NUMBER", length = 15)
    @Schema(description = "핸드폰 번호")
    private String phoneNumber;

    @ColumnDefault("'ROLE_USER'")
    @Column(name = "ROLE", nullable = false)
    @Schema(description = "권한")
    private String role;

    @Setter
    @ColumnDefault("false")
    @Column(name = "ENABLED", nullable = false)
    @Schema(description = "인증여부")
    private Boolean enabled;

    @Column(name = "PROVIDER")
    private String provider;

    @Column(name = "PROVIDER_ID")
    private String providerId;

    public void updateProfile(UserDto.UserProfileReq dto) {
        if (dto.getName() != null) this.name = dto.getName();
        if (dto.getIntroduction() != null) this.introduction = dto.getIntroduction();
        if (dto.getProfileImageUrl() != null) this.profileImageUrl = dto.getProfileImageUrl();
    }

    public void updateProfileImage(String savedFileName) {
        this.profileImageUrl = savedFileName;
    }
}
