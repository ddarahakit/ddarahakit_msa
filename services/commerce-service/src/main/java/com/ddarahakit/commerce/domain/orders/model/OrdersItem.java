package com.ddarahakit.commerce.domain.orders.model;

import com.ddarahakit.commerce.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "orders_item",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"orders_idx", "course_idx"})
        }
)
public class OrdersItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_idx")
    private Orders orders;

    // MSA: Course 엔티티 대신 평문 FK(courseIdx) + 결제 시점 스냅샷 보유.
    @Column(name = "course_idx")
    private Long courseIdx;

    // 스냅샷: 결제 시점 코스명/단가(코스가 사라지거나 가격이 바뀌어도 영수증/내역은 불변)
    private String courseName;

    private int unitPrice;
}
