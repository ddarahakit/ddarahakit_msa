package com.ddarahakit.commerce.domain.cart;

import com.ddarahakit.commerce.domain.cart.model.Cart;
import com.ddarahakit.commerce.domain.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    boolean existsByCartAndCourseIdx(Cart cart, Long courseIdx);

    Optional<CartItem> findByIdxAndCart(Long idx, Cart cart);

    int countByCart(Cart cart);
}
