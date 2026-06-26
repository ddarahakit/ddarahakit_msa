package com.ddarahakit.commerce.domain.cart;

import com.ddarahakit.commerce.domain.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdx(Long userIdx);
}
