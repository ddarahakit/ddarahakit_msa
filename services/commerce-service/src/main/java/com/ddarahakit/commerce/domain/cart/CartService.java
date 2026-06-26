package com.ddarahakit.commerce.domain.cart;

import com.ddarahakit.commerce.client.CourseClient;
import com.ddarahakit.commerce.common.exception.BaseException;
import com.ddarahakit.commerce.config.security.AuthUserDetails;
import com.ddarahakit.commerce.domain.cart.model.Cart;
import com.ddarahakit.commerce.domain.cart.model.CartDto;
import com.ddarahakit.commerce.domain.cart.model.CartItem;
import com.ddarahakit.commerce.domain.orders.OrdersItemRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.ddarahakit.commerce.common.model.BaseResponseStatus.*;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseClient courseClient;
    private final OrdersItemRepository ordersItemRepository;

    private Cart getOrCreateCart(Long userIdx) {
        return cartRepository.findByUserIdx(userIdx)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userIdx(userIdx).build()
                ));
    }

    public CartDto.CartCountRes count(AuthUserDetails authUserDetails) {
        Cart cart = getOrCreateCart(authUserDetails.getIdx());
        int count = cartItemRepository.countByCart(cart);
        return CartDto.CartCountRes.of(count);
    }

    public CartDto.CartRes list(AuthUserDetails authUserDetails) {
        Cart cart = getOrCreateCart(authUserDetails.getIdx());
        return CartDto.CartRes.of(cart.getCartItems());
    }

    @Transactional
    public CartDto.CartItemRes add(AuthUserDetails authUserDetails, CartDto.CartItemReq dto) {
        Long userIdx = authUserDetails.getIdx();

        // 모놀리스에서 코스 조회(존재 검증 + 스냅샷 확보)
        CourseClient.CourseResponse res = courseClient.get(dto.getCourseIdx());
        if (res == null || !res.success() || res.results() == null) {
            throw BaseException.of(COURSE_NOT_FOUND);
        }
        CourseClient.CourseInfo course = res.results();

        Cart cart = getOrCreateCart(userIdx);

        boolean alreadyPurchased = ordersItemRepository
                .existsByOrdersUserIdxAndOrdersPaidTrueAndOrdersRefundedFalseAndCourseIdx(userIdx, dto.getCourseIdx());
        if (alreadyPurchased) {
            throw BaseException.of(CART_ALREADY_PURCHASED);
        }

        if (cartItemRepository.existsByCartAndCourseIdx(cart, dto.getCourseIdx())) {
            throw BaseException.of(CART_ALREADY_EXISTS);
        }

        CartItem cartItem = cartItemRepository.save(dto.toEntity(cart, course));

        return CartDto.CartItemRes.of(cartItem);
    }

    @Transactional
    public CartDto.CartItemRes remove(AuthUserDetails authUserDetails, Long cartItemIdx) {
        Cart cart = getOrCreateCart(authUserDetails.getIdx());

        CartItem cartItem = cartItemRepository.findByIdxAndCart(cartItemIdx, cart).orElseThrow(
                () -> BaseException.of(CART_NOT_FOUND)
        );

        cartItemRepository.delete(cartItem);
        return CartDto.CartItemRes.of(cartItem);
    }

    @Transactional
    public void clear(AuthUserDetails authUserDetails) {
        Cart cart = getOrCreateCart(authUserDetails.getIdx());
        cart.getCartItems().clear();
    }
}
