package pandq.application.port.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    List<Order> findAll();
    List<Order> findByUserId(UUID userId);
    Page<Order> searchUserOrders(UUID userId, OrderStatus status, String query, UUID orderId, Pageable pageable);
    void deleteById(UUID id);
}
