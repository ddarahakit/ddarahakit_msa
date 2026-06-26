package com.ddarahakit.commerce.domain.cart.model;

import com.ddarahakit.commerce.client.CourseClient;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class CartDto {

    @Builder
    @Data
    public static class CartItemReq {
        @NotNull(message = "코스 IDX는 필수 입력값입니다.")
        @Min(value = 1, message = "코스 IDX는 1 이상이어야 합니다.")
        @Schema(description = "코스 IDX", required = true, example = "1")
        private Long courseIdx;

        public CartItem toEntity(Cart cart, CourseClient.CourseInfo course) {
            return CartItem.builder()
                    .cart(cart)
                    .courseIdx(course.idx())
                    .courseName(course.name())
                    .courseImage(course.image())
                    .salePrice(course.salePrice())
                    .originalPrice(course.originalPrice())
                    .build();
        }
    }

    @Builder
    @Data
    public static class CartItemRes {
        @Schema(description = "장바구니 항목 IDX")
        private Long cartItemIdx;
        @Schema(description = "코스 IDX")
        private Long courseIdx;
        @Schema(description = "코스 이름")
        private String courseName;
        @Schema(description = "코스 이미지")
        private String courseImage;
        @Schema(description = "코스 원래 가격")
        private int originalPrice;
        @Schema(description = "코스 할인 가격")
        private int salePrice;

        public static CartItemRes of(CartItem entity) {
            return CartItemRes.builder()
                    .cartItemIdx(entity.getIdx())
                    .courseIdx(entity.getCourseIdx())
                    .courseName(entity.getCourseName())
                    .courseImage(entity.getCourseImage())
                    .originalPrice(entity.getOriginalPrice())
                    .salePrice(entity.getSalePrice())
                    .build();
        }
    }

    @Builder
    @Data
    public static class CartCountRes {
        @Schema(description = "장바구니 항목 수")
        private int count;

        public static CartCountRes of(int count) {
            return CartCountRes.builder()
                    .count(count)
                    .build();
        }
    }

    @Builder
    @Data
    public static class CartRes {
        @Schema(description = "장바구니 항목 목록")
        private List<CartItemRes> cartItems;
        @Schema(description = "총 원래 가격")
        private int totalOriginalPrice;
        @Schema(description = "총 할인 가격")
        private int totalSalePrice;

        public static CartRes of(List<CartItem> entities) {
            List<CartItemRes> cartItemResList = entities.stream()
                    .map(CartItemRes::of)
                    .toList();

            int totalOriginalPrice = entities.stream()
                    .mapToInt(CartItem::getOriginalPrice)
                    .sum();

            int totalSalePrice = entities.stream()
                    .mapToInt(CartItem::getSalePrice)
                    .sum();

            return CartRes.builder()
                    .cartItems(cartItemResList)
                    .totalOriginalPrice(totalOriginalPrice)
                    .totalSalePrice(totalSalePrice)
                    .build();
        }
    }
}
