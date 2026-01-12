package pandq.application.port.repositories;

import pandq.domain.models.chat.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for ChatMessage repository operations.
 */
public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findById(UUID id);

    /**
     * Get all messages in a chat conversation.
     */
    List<ChatMessage> findByProductChatId(UUID productChatId);

    /**
     * Get messages in a chat ordered by timestamp (oldest first).
     */
    List<ChatMessage> findByProductChatIdOrderByCreatedAt(UUID productChatId);

    /**
     * Get unread messages in a chat for a specific user.
     */
    List<ChatMessage> findUnreadByProductChatIdAndNotSender(UUID productChatId, UUID senderId);

    /**
     * Get latest message in a chat.
     */
    Optional<ChatMessage> findLatestByProductChatId(UUID productChatId);

    /**
     * Count unread messages for a user in a specific chat.
     */
    long countUnreadByProductChatIdAndNotSender(UUID productChatId, UUID senderId);

    /**
     * Mark all messages as read in a chat.
     */
    void markAllAsReadByProductChatId(UUID productChatId);

    void deleteById(UUID id);

    void deleteByProductChatId(UUID productChatId);
}
