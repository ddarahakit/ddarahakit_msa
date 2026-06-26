package com.ddarahakit.commerce.domain.cart;

import com.ddarahakit.commerce.common.model.BaseResponse;
import com.ddarahakit.commerce.config.security.AuthUserDetails;
import com.ddarahakit.commerce.domain.cart.model.CartDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping(value = "/cart")
@RestController
@Tag(name = "장바구니 컨트롤러")
public class CartController {

    private final CartService cartService;

    @Operation(
            summary = "장바구니 항목 수 조회",
            description = "로그인한 사용자의 장바구니 항목 수를 조회합니다.")
    @GetMapping("/count")
    public ResponseEntity<BaseResponse<CartDto.CartCountRes>> count(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        CartDto.CartCountRes response = cartService.count(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "장바구니 목록 조회",
            description = "로그인한 사용자의 장바구니 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<BaseResponse<CartDto.CartRes>> list(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        CartDto.CartRes response = cartService.list(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "장바구니 항목 추가",
            description = "코스 IDX로 장바구니에 항목을 추가합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<CartDto.CartItemRes>> add(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @Valid @RequestBody CartDto.CartItemReq dto) {
        CartDto.CartItemRes response = cartService.add(authUserDetails, dto);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "장바구니 항목 삭제",
            description = "장바구니 항목 IDX로 해당 항목을 삭제합니다.")
    @DeleteMapping("/{cartItemIdx}")
    public ResponseEntity<BaseResponse<CartDto.CartItemRes>> remove(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long cartItemIdx) {
        CartDto.CartItemRes response = cartService.remove(authUserDetails, cartItemIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "장바구니 비우기",
            description = "로그인한 사용자의 장바구니를 전체 비웁니다.")
    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> clear(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        cartService.clear(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
