package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.ChatMessageRepository;
import pandq.domain.models.chat.ChatMessage;
import pandq.infrastructure.persistence.repositories.jpa.JpaChatMessageRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ChatMessageRepository using JPA.
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final JpaChatMessageRepository jpaRepository;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        return jpaRepository.save(chatMessage);
    }

    @Override
    public Optional<ChatMessage> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ChatMessage> findByProductChatId(UUID productChatId) {
        return jpaRepository.findByProductChatId(productChatId);
    }

    @Override
    public List<ChatMessage> findByProductChatIdOrderByCreatedAt(UUID productChatId) {
        return jpaRepository.findByProductChatIdOrderByCreatedAt(productChatId);
    }

    @Override
    public List<ChatMessage> findUnreadByProductChatIdAndNotSender(UUID productChatId, UUID senderId) {
        return jpaRepository.findUnreadByProductChatIdAndNotSender(productChatId, senderId);
    }

    @Override
    public Optional<ChatMessage> findLatestByProductChatId(UUID productChatId) {
        return jpaRepository.findLatestByProductChatId(productChatId);
    }

    @Override
    public long countUnreadByProductChatIdAndNotSender(UUID productChatId, UUID senderId) {
        return jpaRepository.countUnreadByProductChatIdAndNotSender(productChatId, senderId);
    }

    @Override
    public void markAllAsReadByProductChatId(UUID productChatId) {
        List<ChatMessage> messages = jpaRepository.findByProductChatId(productChatId);
        messages.forEach(m -> {
            if (!m.isRead()) {
                m.markAsRead();
                jpaRepository.save(m);
            }
        });
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByProductChatId(UUID productChatId) {
        jpaRepository.deleteByProductChatId(productChatId);
    }
}
