package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;

import java.util.List;
import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN o.orderItems oi LEFT JOIN oi.product p " +
           "WHERE o.user.id = :userId " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:orderId IS NULL OR o.id = :orderId)")
    Page<Order> searchUserOrders(@Param("userId") UUID userId,
                                 @Param("status") OrderStatus status,
                                 @Param("q") String q,
                                 @Param("orderId") UUID orderId,
                                 Pageable pageable);
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);
}
