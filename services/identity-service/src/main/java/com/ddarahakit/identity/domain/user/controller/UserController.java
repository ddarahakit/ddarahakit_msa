package com.ddarahakit.identity.domain.user.controller;

import com.ddarahakit.identity.common.model.BaseResponse;
import com.ddarahakit.identity.config.security.AuthUserDetails;
import com.ddarahakit.identity.config.security.JwtProvider;
import com.ddarahakit.identity.config.security.token.TokenService;
import com.ddarahakit.identity.config.security.token.TokenService.TokenPair;
import com.ddarahakit.identity.domain.user.model.dto.UserDto;
import com.ddarahakit.identity.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import static com.ddarahakit.identity.common.model.BaseResponseStatus.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/user")
@Tag(name = "회원 컨트롤러")
public class UserController {

    private static final String ACCESS_TOKEN_COOKIE = "ATOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "RTOKEN";

    @Value("${app.token.access-format}")
    private String accessFormat;

    @Value("${app.token.refresh-format}")
    private String refreshFormat;

    @Value("${app.token.access-max-age}")
    private int accessMaxAge;

    @Value("${app.token.refresh-max-age}")
    private int refreshMaxAge;

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자가 자신의 프로필을 조회한다.")
    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<UserDto.UserProfileRes>> dashboard(
            @AuthenticationPrincipal AuthUserDetails authUserDetails
    ) {
        UserDto.UserProfileRes response = userService.getUserProfile(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "현재 로그인한 사용자가 자신의 프로필을 수정한다.")
    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<UserDto.UserProfileRes>> editProfile(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @RequestBody UserDto.UserProfileReq dto
    ) {
        UserDto.UserProfileRes response = userService.updateUserProfile(authUserDetails, dto);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "사용자 토큰 갱신",
            description = "리프레시 토큰으로 새로운 토큰 쌍을 발급받는다. (Refresh Token Rotation)")
    @GetMapping("/token/refresh")
    public ResponseEntity<BaseResponse<UserDto.LoginRes>> tokenRefresh(
            @CookieValue(value = "RTOKEN", required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BaseResponse.error(INVALID_JWT));
        }

        // Refresh Token Rotation: 새 토큰 쌍 발급, 기존 토큰 무효화
        TokenPair tokenPair = tokenService.refreshTokenPair(refreshToken, request);
        String userEmail = jwtProvider.extractEmail(tokenPair.accessToken());
        AuthUserDetails authUserDetails = (AuthUserDetails) userService.loadUserByUsername(userEmail);
        return createLoginResponse(authUserDetails, tokenPair);
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인한다.")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<UserDto.LoginRes>> login(
            @Valid @RequestBody UserDto.LoginReq dto,
            HttpServletRequest request
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );
        AuthUserDetails authUserDetails = (AuthUserDetails) authentication.getPrincipal();
        return doLogin(authUserDetails, request);
    }

    @Operation(
            summary = "소셜 로그인 콜백",
            description = "소셜 로그인 후 토큰을 발급한다.")
    @PostMapping("/social/login")
    public ResponseEntity<BaseResponse<UserDto.LoginRes>> loginSns(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            HttpServletRequest request
    ) {
        return doLogin(authUserDetails, request);
    }

    @NonNull
    private ResponseEntity<BaseResponse<UserDto.LoginRes>> doLogin(
            AuthUserDetails authUserDetails,
            HttpServletRequest request
    ) {
        // TokenService를 통해 토큰 쌍 생성 (DB에 리프레시 토큰 저장)
        TokenPair tokenPair = tokenService.createTokenPair(authUserDetails, request);
        return createLoginResponse(authUserDetails, tokenPair);
    }

    private ResponseEntity<BaseResponse<UserDto.LoginRes>> createLoginResponse(
            AuthUserDetails authUserDetails,
            TokenPair tokenPair
    ) {
        // Max-Age 는 초 단위. 설정값(access/refresh-max-age)은 밀리초이므로 /1000 (OAuth 성공 핸들러와 동일 규칙).
        String accessCookie = String.format(accessFormat, tokenPair.accessToken(), accessMaxAge / 1000);
        String refreshCookie = String.format(refreshFormat, tokenPair.refreshToken(), refreshMaxAge / 1000);

        // JWT 기반 principal(소셜 로그인 등)은 프로필 이미지를 담지 않으므로
        // DB에서 최신 사용자 정보를 다시 읽어 응답(프로필 이미지 포함)을 구성한다.
        AuthUserDetails freshUser = (AuthUserDetails) userService.loadUserByUsername(authUserDetails.getEmail());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie)
                .header("Set-Cookie", refreshCookie)
                .body(BaseResponse.success(UserDto.LoginRes.of(freshUser.toEntity())));
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 세션의 토큰을 무효화하고 로그아웃한다.")
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(
            @CookieValue(value = "RTOKEN", required = false) String refreshToken
    ) {
        // 리프레시 토큰 무효화 (DB에서)
        tokenService.logout(refreshToken);

        // 쿠키 삭제
        String accessCookie = String.format(accessFormat, "", 0);
        String refreshCookie = String.format(refreshFormat, "", 0);

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie)
                .header("Set-Cookie", refreshCookie)
                .body(BaseResponse.success("로그아웃 성공"));
    }

    @Operation(
            summary = "모든 기기에서 로그아웃",
            description = "사용자의 모든 세션을 무효화한다.")
    @PostMapping("/logout/all")
    public ResponseEntity<BaseResponse<String>> logoutAll(
            @AuthenticationPrincipal AuthUserDetails authUserDetails
    ) {
        tokenService.logoutAll(authUserDetails.getIdx());

        String accessCookie = String.format(accessFormat, "", 0);
        String refreshCookie = String.format(refreshFormat, "", 0);

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie)
                .header("Set-Cookie", refreshCookie)
                .body(BaseResponse.success("모든 기기에서 로그아웃 성공"));
    }

    @Operation(
            summary = "이메일 중복 체크",
            description = "입력받은 이메일로 회원가입한 적 있는지 확인")
    @GetMapping("/email/duplicate")
    public ResponseEntity<?> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicated = userService.isEmailDuplicated(email);
        if (isDuplicated) {
            return ResponseEntity.ok().body(BaseResponse.error(DUPLICATE_USER_EMAIL));
        }
        return ResponseEntity.ok().body(BaseResponse.success(isDuplicated));
    }

    @Operation(
            summary = "회원가입",
            description = "이메일과 비밀번호, 회원 이름을 입력해서 회원가입을 진행한다.")
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserDto.SignupRes>> signup(
            @Valid @RequestBody UserDto.SignupReq request
    ) {
        UserDto.SignupRes response = userService.signup(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "회원가입 후 인증",
            description = "이메일과 UUID을 입력받아서 회원을 활성화한다.")
    @PostMapping("/email/verify")
    public ResponseEntity<BaseResponse<UserDto.SignupRes>> signupVerify(
            @Valid @RequestBody UserDto.SignupVerifyReq dto
    ) {
        UserDto.SignupRes response = userService.signupVerify(dto);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "토큰 확인",
            description = "액세스 토큰, 리프레시 토큰의 유효성을 확인한다.")
    @GetMapping("/check")
    public ResponseEntity<?> checkLogin(HttpServletRequest request) {
        String accessToken = extractTokenFromCookie(request, ACCESS_TOKEN_COOKIE);
        String refreshToken = extractTokenFromCookie(request, REFRESH_TOKEN_COOKIE);

        // 액세스 토큰이 유효하면 성공
        if (accessToken != null && jwtProvider.validateToken(accessToken) && jwtProvider.isAccessToken(accessToken)) {
            return ResponseEntity.ok(BaseResponse.success(true));
        }

        // 액세스 토큰 만료, 리프레시 토큰으로 재발급 시도
        if (refreshToken != null && jwtProvider.validateToken(refreshToken) && jwtProvider.isRefreshToken(refreshToken)) {
            TokenPair tokenPair = tokenService.refreshTokenPair(refreshToken, request);
            String userEmail = jwtProvider.extractEmail(tokenPair.accessToken());
            AuthUserDetails authUserDetails = (AuthUserDetails) userService.loadUserByUsername(userEmail);
            return createLoginResponse(authUserDetails, tokenPair);
        }

        // 둘 다 유효하지 않음
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(INVALID_JWT));
    }

    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 로그인한 사용자의 비밀번호를 변경한다.")
    @PutMapping("/password/update")
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @Valid @RequestBody UserDto.UpdatePasswordReq dto
    ) {
        UserDto.SignupRes response = userService.updatePassword(authUserDetails, dto);

        // 비밀번호 변경 시 모든 세션 무효화 (보안 강화)
        tokenService.logoutAll(authUserDetails.getIdx());

        return ResponseEntity.ok().body(BaseResponse.success(response));
    }

    @Operation(
            summary = "UUID 만료 확인",
            description = "프론트에서 페이지 접속했을 때 UUID가 만료된건지 아닌지 확인.")
    @GetMapping("/uuid/check")
    public ResponseEntity<?> checkUuidExpired(
            @RequestParam String email,
            @RequestParam String uuid
    ) {
        boolean response = userService.checkUuidExpired(email, uuid);
        return ResponseEntity.ok().body(BaseResponse.success(response));
    }

    @Operation(
            summary = "비밀번호 찾기 요청",
            description = "가입한 사용자의 이메일로 비밀번호 변경 링크를 전송한다.")
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody UserDto.ResetPasswordEmailReq dto
    ) {
        userService.resetPasswordEmailReq(dto);
        return ResponseEntity.ok().body(BaseResponse.success(true));
    }

    @Operation(
            summary = "비밀번호 찾기 요청 처리",
            description = "새로운 비밀번호로 비밀번호를 변경한다.")
    @PutMapping("/password/reset")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody UserDto.ResetPasswordReq dto
    ) {
        UserDto.SignupRes response = userService.resetPassword(dto);
        // 비밀번호 재설정 후 해당 사용자의 모든 세션(리프레시 토큰)을 무효화한다.
        // (계정 탈취 의심 상황에서 쓰이는 흐름이므로, 공격자의 기존 세션이 살아있지 않게 한다)
        tokenService.logoutAll(response.getIdx());
        return ResponseEntity.ok().body(BaseResponse.success(response));
    }

    @PostMapping("/profile")
    public ResponseEntity<?> updateProfileImage(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            MultipartFile profileImage
    ) {
        UserDto.UserProfileRes response = userService.updateUserProfile(authUserDetails, profileImage);
        return ResponseEntity.ok().body(BaseResponse.success(response));
    }
}
