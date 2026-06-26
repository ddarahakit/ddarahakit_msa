package com.ddarahakit.commerce.domain.orders;

import com.ddarahakit.commerce.common.model.BaseResponse;
import com.ddarahakit.commerce.config.security.AuthUserDetails;
import com.ddarahakit.commerce.domain.orders.model.OrdersDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 집계(commerce-service 소유분).
 * 모놀리스 은퇴를 위해 결제 내역 조회를 commerce-service 로 이전한다. 인증은 헤더(X-User-Id) 기반.
 */
@RequiredArgsConstructor
@RestController
@Tag(name = "마이페이지(결제) 컨트롤러")
public class MyController {

    private final OrdersService ordersService;

    @Operation(summary = "결제 내역 조회", description = "현재 사용자의 결제 완료 주문 내역을 페이징(page/size)으로 조회한다.")
    @GetMapping("/user/payments")
    public ResponseEntity<BaseResponse<OrdersDto.PaymentPageRes>> getPayments(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        OrdersDto.PaymentPageRes response = ordersService.payments(authUserDetails, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
