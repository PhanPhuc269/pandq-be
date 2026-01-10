package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.ProductChatRepository;
import pandq.domain.models.chat.ChatStatus;
import pandq.domain.models.chat.ProductChat;
import pandq.domain.models.PaginatedResult;
import pandq.infrastructure.persistence.repositories.jpa.JpaProductChatRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ProductChatRepository using JPA.
 */
@Repository
@RequiredArgsConstructor
public class ProductChatRepositoryImpl implements ProductChatRepository {

    private final JpaProductChatRepository jpaRepository;

    @Override
    public ProductChat save(ProductChat productChat) {
        return jpaRepository.save(productChat);
    }

    @Override
    public Optional<ProductChat> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ProductChat> findActiveByProductAndCustomer(UUID productId, UUID customerId) {
        return jpaRepository.findByProductIdAndCustomerIdAndStatus(productId, customerId, ChatStatus.OPEN)
                .or(() -> jpaRepository.findByProductIdAndCustomerIdAndStatus(productId, customerId, ChatStatus.PENDING));
    }

    @Override
    public List<ProductChat> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId);
    }

    @Override
    public List<ProductChat> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId);
    }

    @Override
    public List<ProductChat> findByAdminId(UUID adminId) {
        return jpaRepository.findByAdminId(adminId);
    }

    @Override
    public List<ProductChat> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ProductChat> findOpenByProductId(UUID productId) {
        return jpaRepository.findByProductIdAndStatus(productId, ChatStatus.OPEN);
    }

    @Override
    public PaginatedResult<ProductChat> findChatsByAdminPaginated(UUID adminId, int page, int size) {
        Page<ProductChat> pageResult = jpaRepository.findByAdminId(adminId, PageRequest.of(page, size));

        return PaginatedResult.of(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public long countByProductId(UUID productId) {
        return jpaRepository.countByProductId(productId);
    }

    @Override
    public long countByAdminId(UUID adminId) {
        return jpaRepository.countByAdminId(adminId);
    }

    @Override
    public Optional<ProductChat> findGeneralChatByCustomer(UUID customerId) {
        return jpaRepository.findGeneralChatByCustomer(customerId);
    }
}
