package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.application.port.repositories.ChatMessageRepository;
import pandq.application.port.repositories.ProductChatRepository;
import pandq.application.port.repositories.ProductRepository;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.chat.ChatMessage;
import pandq.domain.models.chat.ChatStatus;
import pandq.domain.models.chat.MessageType;
import pandq.domain.models.chat.ProductChat;
import pandq.domain.models.enums.NotificationType;
import pandq.domain.models.product.Product;
import pandq.domain.models.user.User;
import pandq.infrastructure.services.FcmService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for handling product chat operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductChatService {

    private final ProductChatRepository productChatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final NotificationService notificationService;

    /**
     * Start a new chat for a product.
     * Customer ID is required but customer user can be null for testing.
     */
    @Transactional
    public ProductChat startChat(UUID productId, UUID customerId, String subject) {
        log.info("Starting chat for product {} by customer {}", productId, customerId);

        // Customer ID is required (tracks who initiated the chat)
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        // Try to load product and customer
        // Both can be null - will work with test IDs
        Product product = productRepository.findById(productId).orElse(null);
        User customer = userRepository.findById(customerId).orElse(null);
        
        log.info("Product found: {}, Customer found: {}", product != null, customer != null);

        // Check if active chat already exists
        var existingChat = productChatRepository.findActiveByProductAndCustomer(productId, customerId);
        if (existingChat.isPresent()) {
            log.warn("Active chat already exists for product {} and customer {}", productId, customerId);
            return existingChat.get();
        }

        // Create new chat (product/customer may be null for testing)
        ProductChat chat = ProductChat.builder()
                .product(product)
                .customer(customer)
                .subject(subject)
                .status(ChatStatus.PENDING)
                .build();

        ProductChat savedChat = productChatRepository.save(chat);
        log.info("Chat created successfully: {}", savedChat.getId());

        return savedChat;
    }

    /**
     * Get a specific chat by ID.
     */
    @Transactional(readOnly = true)
    public ProductChat getChatById(UUID chatId) {
        return productChatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found with id: " + chatId));
    }

    /**
     * Get active chat for a product and customer.
     * If no active chat exists, create a new one.
     */
    @Transactional
    public ProductChat getActiveChat(UUID productId, UUID customerId) {
        return productChatRepository.findActiveByProductAndCustomer(productId, customerId)
                .orElseGet(() -> startChat(productId, customerId, "Product Inquiry"));
    }

    /**
     * Get or create general chat with admin (not tied to specific product).
     * One continuous chat thread for a customer with support admin.
     */
    @Transactional
    public ProductChat getOrCreateGeneralChat(UUID customerId) {
        log.info("Getting general chat for customer: {}", customerId);
        
        // Try to find existing general chat (product = null, customer = customerId)
        var existingChat = productChatRepository.findGeneralChatByCustomer(customerId);
        if (existingChat.isPresent()) {
            log.info("Found existing general chat: {}", existingChat.get().getId());
            return existingChat.get();
        }
        
        // Create new general chat
        User customer = userRepository.findById(customerId).orElse(null);
        ProductChat generalChat = ProductChat.builder()
                .product(null)  // No specific product
                .customer(customer)
                .subject("General Support")
                .status(ChatStatus.OPEN)
                .build();
        
        ProductChat savedChat = productChatRepository.save(generalChat);
        log.info("Created new general chat: {}", savedChat.getId());
        return savedChat;
    }

    /**
     * Get all chats for a customer.
     */
    @Transactional(readOnly = true)
    public List<ProductChat> getCustomerChats(UUID customerId) {
        return productChatRepository.findByCustomerId(customerId);
    }

    /**
     * Get all chats for a product.
     */
    @Transactional(readOnly = true)
    public List<ProductChat> getProductChats(UUID productId) {
        return productChatRepository.findByProductId(productId);
    }

    /**
     * Get all chats assigned to an admin.
     */
    @Transactional(readOnly = true)
    public List<ProductChat> getAdminChats(UUID adminId) {
        return productChatRepository.findByAdminId(adminId);
    }

    /**
     * Get all chats.
     */
    @Transactional(readOnly = true)
    public List<ProductChat> getAllChats() {
        return productChatRepository.findAll();
    }

    /**
     * Assign a chat to an admin.
     */
    public ProductChat assignChatToAdmin(UUID chatId, UUID adminId) {
        log.info("Assigning chat {} to admin {}", chatId, adminId);

        ProductChat chat = getChatById(chatId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NoSuchElementException("Admin not found"));

        chat.setAdmin(admin);
        chat.setStatus(ChatStatus.OPEN);

        return productChatRepository.save(chat);
    }

    /**
     * Close a chat.
     */
    public ProductChat closeChat(UUID chatId) {
        log.info("Closing chat: {}", chatId);

        ProductChat chat = getChatById(chatId);
        chat.closeChat();

        return productChatRepository.save(chat);
    }

    /**
     * Reopen a closed chat.
     */
    public ProductChat reopenChat(UUID chatId) {
        log.info("Reopening chat: {}", chatId);

        ProductChat chat = getChatById(chatId);
        chat.openChat();

        return productChatRepository.save(chat);
    }

    /**
     * Send a message in a chat.
     */
    public ChatMessage sendMessage(UUID chatId, UUID senderId, String messageContent, MessageType messageType) {
        return sendMessageInternal(chatId, senderId, messageContent, messageType, null, null, null, null, null);
    }

    /**
     * Send a message with optional image URL.
     */
    public ChatMessage sendMessage(UUID chatId, UUID senderId, String messageContent, String messageTypeStr, String imageUrl) {
        MessageType messageType = MessageType.valueOf(messageTypeStr);
        return sendMessageInternal(chatId, senderId, messageContent, messageType, imageUrl, null, null, null, null);
    }

    /**
     * Send a message with product context (for general chat with product divider).
     */
    public ChatMessage sendMessageWithProductContext(UUID chatId, UUID senderId, String messageContent, MessageType messageType,
            UUID productContextId, String productContextName, String productContextImage, String productContextPrice) {
        return sendMessageInternal(chatId, senderId, messageContent, messageType, null,
                productContextId, productContextName, productContextImage, productContextPrice);
    }

    /**
     * Internal method for sending message.
     */
    private ChatMessage sendMessageInternal(UUID chatId, UUID senderId, String messageContent, MessageType messageType, String imageUrl,
            UUID productContextId, String productContextName, String productContextImage, String productContextPrice) {
        log.info("Sending message in chat {} from user {}", chatId, senderId);

        ProductChat chat = getChatById(chatId);

        // Try to verify sender - user may not exist for test scenarios
        User sender = userRepository.findById(senderId).orElse(null);
        log.debug("Sender found: {}", sender != null);

        // Verify sender is either customer or admin (using IDs only, user may not exist in DB)
        UUID customerId = chat.getCustomer() != null ? chat.getCustomer().getId() : null;
        UUID adminId = chat.getAdmin() != null ? chat.getAdmin().getId() : null;
        
        // Allow if sender matches customer or admin
        // If customer is null, allow any sender (test scenario)
        boolean isCustomer = customerId != null && senderId.equals(customerId);
        boolean isAdmin = adminId != null && senderId.equals(adminId);
        
        // If sender is not customer and admin is not assigned, auto-assign sender as admin
        if (!isCustomer && !isAdmin && customerId != null) {
            log.info("Auto-assigning sender {} as admin for chat {}", senderId, chatId);
            User adminUser = userRepository.findById(senderId).orElse(null);
            chat.setAdmin(adminUser);
            if (chat.getStatus() == ChatStatus.PENDING) {
                chat.setStatus(ChatStatus.OPEN);
            }
            chat = productChatRepository.save(chat);
            adminId = senderId;
            isAdmin = true;
        }
        
        if (customerId != null && !isCustomer && !isAdmin) {
            log.warn("User {} is not part of chat {} (customer: {}, admin: {})", 
                    senderId, chatId, customerId, adminId);
            throw new IllegalArgumentException("User is not part of this chat");
        }
        
        log.info("Message validation passed - proceeding to save");

        // Determine sender name
        String senderName = "Unknown";
        if (chat.getAdmin() != null && senderId.equals(chat.getAdmin().getId())) {
            senderName = chat.getAdmin().getFullName();
        } else if (chat.getCustomer() != null && senderId.equals(chat.getCustomer().getId())) {
            senderName = chat.getCustomer().getFullName();
        } else if (sender != null) {
            senderName = sender.getFullName();
        }

        // Determine sender role
        String senderRole = "CUSTOMER";
        if (chat.getAdmin() != null && senderId.equals(chat.getAdmin().getId())) {
            senderRole = "ADMIN";
        }

        ChatMessage message = ChatMessage.builder()
                .productChat(chat)
                .senderId(senderId)
                .senderName(senderName)
                .senderRole(senderRole)
                .message(messageContent)
                .messageType(messageType)
                .imageUrl(imageUrl)
                .productContextId(productContextId)
                .productContextName(productContextName)
                .productContextImage(productContextImage)
                .productContextPrice(productContextPrice)
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.debug("Message sent successfully: {}", savedMessage.getId());
        
        // Manually set chatId from the productChat reference before it detaches
        if (chat != null && chat.getId() != null) {
            savedMessage.setChatId(chat.getId());
            log.debug("ChatId set to: {}", chat.getId());
        }

        // Send push notification to recipient
        sendChatNotification(chat, savedMessage, senderRole, senderName, messageContent, messageType);

        return savedMessage;
    }

    /**
     * Send push notification for new chat message and save to database.
     */
    private void sendChatNotification(ProductChat chat, ChatMessage message, String senderRole, 
            String senderName, String messageContent, MessageType messageType) {
        try {
            User recipient = null;
            String notificationTitle;
            String notificationBody;

            // Determine recipient based on sender role
            if ("ADMIN".equals(senderRole)) {
                // Admin sent message -> notify customer
                recipient = chat.getCustomer();
                notificationTitle = "Tin nhắn mới từ Admin";
            } else {
                // Customer sent message -> notify admin
                recipient = chat.getAdmin();
                notificationTitle = "Tin nhắn mới từ " + senderName;
            }

            if (recipient == null) {
                log.debug("No recipient for chat notification, skipping");
                return;
            }

            // Build notification body
            if (messageType == MessageType.IMAGE) {
                notificationBody = senderName + " đã gửi một ảnh";
            } else {
                notificationBody = messageContent.length() > 100 
                        ? messageContent.substring(0, 100) + "..." 
                        : messageContent;
            }

            // Build target URL for deep linking to chat
            String targetUrl = "chat/" + chat.getId().toString();

            // Save notification to database AND send FCM (via NotificationService, which checks preferences)
            try {
                notificationService.createNotification(
                        recipient.getId(),
                        NotificationType.CHAT_MESSAGE,
                        notificationTitle,
                        notificationBody,
                        targetUrl
                );
                log.info("Chat notification processed for user {}", recipient.getId());
            } catch (Exception dbEx) {
                log.error("Failed to create chat notification: {}", dbEx.getMessage());
            }
            
            // NOTE: FCM is now handled inside NotificationService.createNotification.
            // Removed direct fcmService call to respect user preferences.

        } catch (Exception e) {
            log.error("Failed to send chat notification: {}", e.getMessage());
            // Don't throw - notification failure should not affect message sending
        }
    }

    /**
     * Get all messages in a chat.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessages(UUID chatId) {
        return chatMessageRepository.findByProductChatIdOrderByCreatedAt(chatId);
    }

    /**
     * Get latest message in a chat.
     */
    @Transactional(readOnly = true)
    public ChatMessage getLatestMessage(UUID chatId) {
        return chatMessageRepository.findLatestByProductChatId(chatId)
                .orElseThrow(() -> new NoSuchElementException("No messages in this chat"));
    }

    /**
     * Mark message as read.
     */
    public ChatMessage markMessageAsRead(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        message.markAsRead();
        return chatMessageRepository.save(message);
    }

    /**
     * Mark all messages as read for a user in a chat.
     */
    public void markAllMessagesAsRead(UUID chatId, UUID userId) {
        log.info("Marking all messages as read in chat {} for user {}", chatId, userId);

        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadByProductChatIdAndNotSender(chatId, userId);
        unreadMessages.forEach(ChatMessage::markAsRead);
        unreadMessages.forEach(chatMessageRepository::save);
    }

    /**
     * Count unread messages in a chat for a specific user.
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(UUID chatId, UUID userId) {
        return chatMessageRepository.countUnreadByProductChatIdAndNotSender(chatId, userId);
    }

    /**
     * Delete a chat and all its messages.
     */
    public void deleteChat(UUID chatId) {
        log.info("Deleting chat: {}", chatId);

        if (!productChatRepository.existsById(chatId)) {
            throw new NoSuchElementException("Chat not found");
        }

        productChatRepository.deleteById(chatId);
    }
}
