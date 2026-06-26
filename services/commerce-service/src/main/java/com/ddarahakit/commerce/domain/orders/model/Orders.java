package com.ddarahakit.commerce.domain.orders.model;


import com.ddarahakit.commerce.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "orders",
        // paymentId 유니크: 동일 포트원 결제건이 서로 다른 주문에 이중 확정되는 것을 DB 차원에서 방지.
        // 미결제 주문의 paymentId 는 NULL 이며 MariaDB 는 유니크 인덱스에서 다중 NULL 을 허용한다.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_payment_id", columnNames = {"paymentId"})
        }
)
public class Orders extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @ColumnDefault("false")
    private Boolean paid;

    private int paymentPrice;

    // MSA: User 엔티티 대신 평문 FK(userIdx)만 보유한다.
    @Column(name = "user_idx")
    private Long userIdx;

    @Builder.Default
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdersItem> items = new ArrayList<>();

    @Setter
    private String paymentId;

    @Setter
    @ColumnDefault("false")
    private Boolean refunded;
}
