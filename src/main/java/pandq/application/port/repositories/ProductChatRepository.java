package pandq.application.port.repositories;

import pandq.domain.models.chat.ProductChat;
import pandq.domain.models.PaginatedResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for ProductChat repository operations.
 */
public interface ProductChatRepository {

    ProductChat save(ProductChat productChat);

    Optional<ProductChat> findById(UUID id);

    /**
     * Find active chat between customer and product.
     * Returns the most recent chat if multiple exist.
     */
    Optional<ProductChat> findActiveByProductAndCustomer(UUID productId, UUID customerId);

    /**
     * Find general chat for customer (product = null, customer = customerId).
     * Returns one continuous thread with support admin.
     */
    Optional<ProductChat> findGeneralChatByCustomer(UUID customerId);

    /**
     * Get all chats for a specific product.
     */
    List<ProductChat> findByProductId(UUID productId);

    /**
     * Get all chats for a specific customer.
     */
    List<ProductChat> findByCustomerId(UUID customerId);

    /**
     * Get all chats assigned to an admin.
     */
    List<ProductChat> findByAdminId(UUID adminId);

    /**
     * Get all chats.
     */
    List<ProductChat> findAll();

    /**
     * Get all open chats for a product.
     */
    List<ProductChat> findOpenByProductId(UUID productId);

    /**
     * Get paginated chats for admin dashboard.
     */
    PaginatedResult<ProductChat> findChatsByAdminPaginated(UUID adminId, int page, int size);

    /**
     * Delete a chat and all its messages.
     */
    void deleteById(UUID id);

    boolean existsById(UUID id);

    long countByProductId(UUID productId);

    long countByAdminId(UUID adminId);
}
