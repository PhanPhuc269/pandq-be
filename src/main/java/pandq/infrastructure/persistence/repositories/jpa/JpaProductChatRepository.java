package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pandq.domain.models.chat.ProductChat;
import pandq.domain.models.chat.ChatStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for ProductChat.
 */
@Repository
public interface JpaProductChatRepository extends JpaRepository<ProductChat, UUID> {

    Optional<ProductChat> findByIdAndStatus(UUID id, ChatStatus status);

    Optional<ProductChat> findByProductIdAndCustomerIdAndStatus(UUID productId, UUID customerId, ChatStatus status);

    List<ProductChat> findByProductId(UUID productId);

    List<ProductChat> findByCustomerId(UUID customerId);

    List<ProductChat> findByAdminId(UUID adminId);

    Page<ProductChat> findByAdminId(UUID adminId, Pageable pageable);

    List<ProductChat> findByProductIdAndStatus(UUID productId, ChatStatus status);

    long countByProductId(UUID productId);

    long countByAdminId(UUID adminId);

    @Query(value = "SELECT * FROM product_chats WHERE product_id IS NULL AND user_id = :customerId ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<ProductChat> findGeneralChatByCustomer(@Param("customerId") UUID customerId);
}
