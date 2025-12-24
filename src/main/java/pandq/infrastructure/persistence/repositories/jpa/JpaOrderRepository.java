package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;

import java.util.List;
import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
        List<Order> findByUserId(UUID userId);

        List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

        /**
         * Check if user has purchased a specific product with COMPLETED status
         */
        @Query("SELECT COUNT(o) > 0 FROM Order o " +
                        "JOIN o.orderItems oi " +
                        "WHERE o.user.id = :userId " +
                        "AND oi.product.id = :productId " +
                        "AND o.status = 'COMPLETED'")
        boolean existsByUserIdAndProductIdWithDeliveredStatus(
                        @Param("userId") UUID userId,
                        @Param("productId") UUID productId);
}
