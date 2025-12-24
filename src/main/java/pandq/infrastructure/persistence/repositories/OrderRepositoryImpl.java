package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.OrderRepository;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;
import pandq.infrastructure.persistence.repositories.jpa.JpaOrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    @Override
    public Order save(Order order) {
        return jpaOrderRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaOrderRepository.findById(id);
    }

    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll();
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return jpaOrderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status) {
        return jpaOrderRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public void deleteById(UUID id) {
        jpaOrderRepository.deleteById(id);
    }

    @Override
    public boolean hasUserPurchasedProduct(UUID userId, UUID productId) {
        return jpaOrderRepository.existsByUserIdAndProductIdWithDeliveredStatus(userId, productId);
    }
}
