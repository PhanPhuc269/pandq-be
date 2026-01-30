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

        List<Order> findByStatus(OrderStatus status);

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

        @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o " +
                        "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
                        "AND o.status = 'COMPLETED' " +
                        "AND o.user.id NOT IN (" +
                        "    SELECT p.user.id FROM Order p " +
                        "    WHERE p.createdAt < :startDate " +
                        "    AND p.status = 'COMPLETED'" +
                        ")")
        long countNewCustomersInRange(
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

        /**
         * Get orders with CONFIRMED or SHIPPING status (orders that need inventory reserved)
         */
        @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'SHIPPING')")
        List<Order> findActiveOrdersWithReservedInventory();
}
