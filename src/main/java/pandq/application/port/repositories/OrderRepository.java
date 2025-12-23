package pandq.application.port.repositories;

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
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);
    void deleteById(UUID id);
}
