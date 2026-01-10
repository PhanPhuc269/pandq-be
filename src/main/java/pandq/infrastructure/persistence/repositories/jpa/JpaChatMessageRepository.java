package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pandq.domain.models.chat.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for ChatMessage.
 */
@Repository
public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByProductChatId(UUID productChatId);

    List<ChatMessage> findByProductChatIdOrderByCreatedAt(UUID productChatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.productChat.id = :productChatId AND m.isRead = false AND m.senderId != :senderId ORDER BY m.createdAt ASC")
    List<ChatMessage> findUnreadByProductChatIdAndNotSender(UUID productChatId, UUID senderId);

    @Query("SELECT m FROM ChatMessage m WHERE m.productChat.id = :productChatId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<ChatMessage> findLatestByProductChatId(UUID productChatId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.productChat.id = :productChatId AND m.isRead = false AND m.senderId != :senderId")
    long countUnreadByProductChatIdAndNotSender(UUID productChatId, UUID senderId);

    void deleteByProductChatId(UUID productChatId);
}
