package com.ddarahakit.commerce.domain.orders;

import com.ddarahakit.commerce.domain.orders.model.OrdersItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrdersItemRepository extends JpaRepository<OrdersItem, Long> {
    boolean existsByOrdersUserIdxAndOrdersPaidTrueAndOrdersRefundedFalseAndCourseIdx(Long userIdx, Long courseIdx);

    List<OrdersItem> findByOrdersUserIdxAndOrdersPaidTrueAndOrdersRefundedFalse(Long userIdx);

    // 코스별 주문 항목 수 집계 (popular 정렬용). 코스 컬렉션을 메모리에 적재하지 않고 DB 에서 COUNT.
    // 반환: [courseIdx(Long), count(Long)]. 주문 0건 코스는 행이 없으므로 호출부에서 0 으로 처리.
    @Query("SELECT oi.courseIdx, COUNT(oi) FROM OrdersItem oi GROUP BY oi.courseIdx")
    List<Object[]> countGroupByCourse();
}
