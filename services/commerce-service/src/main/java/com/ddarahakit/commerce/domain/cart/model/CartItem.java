package com.ddarahakit.commerce.domain.cart.model;

import com.ddarahakit.commerce.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "cart_item",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"cart_idx", "course_idx"})
        }
)
public class CartItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDX", nullable = false)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_idx")
    private Cart cart;

    // MSA: Course 엔티티 대신 평문 FK(courseIdx) + 담은 시점 스냅샷 보유.
    @Column(name = "course_idx")
    private Long courseIdx;

    // 스냅샷: 장바구니 표시는 코스 서비스 호출 없이 담은 시점 정보로 렌더링한다.
    private String courseName;

    private String courseImage;

    private int salePrice;

    private int originalPrice;
}
