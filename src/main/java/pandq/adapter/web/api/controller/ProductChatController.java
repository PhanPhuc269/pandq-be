package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.chat.*;
import pandq.application.port.repositories.UserRepository;
import pandq.application.services.ProductChatService;
import pandq.domain.models.chat.ChatMessage;
import pandq.domain.models.chat.MessageType;
import pandq.domain.models.chat.ProductChat;
import pandq.infrastructure.services.CloudinaryService;

import java.math.BigDecimal;import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for product chat operations.
 */
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Slf4j
public class ProductChatController {

    private final ProductChatService productChatService;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Extract user ID from authentication (Firebase UID), with fallback to test user
     * Looks up the Firebase UID in the database to get the actual user ID
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            String firebaseUid = authentication.getName();
            
            // Try to find user by Firebase UID
            try {
                var userOptional = userRepository.findByFirebaseUid(firebaseUid);
                if (userOptional.isPresent()) {
                    UUID userId = userOptional.get().getId();
                    log.debug("Found user {} for Firebase UID: {}", userId, firebaseUid);
                    return userId;
                } else {
                    log.warn("No user found for Firebase UID: {}, using test user UUID", firebaseUid);
                }
            } catch (Exception e) {
                log.error("Error looking up user by Firebase UID: {}", firebaseUid, e);
            }
        }
        
        log.warn("No authentication or Firebase UID found, using test user UUID");
        return UUID.fromString("00000000-0000-0000-0000-000000000001"); // Test user ID
    }

    /**
     * Start a new chat for a specific product.
     * POST /api/v1/chats/products/{productId}/start
     */
    @PostMapping("/products/{productId}/start")
    public ResponseEntity<ProductChatDTO> startChat(
            @PathVariable String productId,
            @RequestBody StartChatRequestDTO request,
            Authentication authentication) {

        UUID customerId = extractUserId(authentication);

        // Try to parse productId, use null if invalid UUID
        UUID productUUID = null;
        try {
            productUUID = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for productId: {}", productId);
        }

        ProductChat chat = productChatService.startChat(productUUID, customerId, request.getSubject());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(chat));
    }

    /**
     * Get active chat for a product (customer view).
     * GET /api/v1/chats/products/{productId}
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductChatDTO> getActiveChat(
            @PathVariable String productId,
            Authentication authentication) {

        UUID customerId = extractUserId(authentication);

        UUID productUUID = null;
        try {
            productUUID = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for productId: {}", productId);
        }

        ProductChat chat = productChatService.getActiveChat(productUUID, customerId);

        return ResponseEntity.ok(mapToDTO(chat));
    }

    /**
     * Get general chat with admin (continuous thread, not product-specific).
     * GET /api/v1/chats/general
     */
    @GetMapping("/general")
    public ResponseEntity<ProductChatDTO> getGeneralChat(Authentication authentication) {
        UUID customerId = extractUserId(authentication);
        // Get or create general chat for this customer (chatId is null, meaning it's general)
        ProductChat chat = productChatService.getOrCreateGeneralChat(customerId);
        return ResponseEntity.ok(mapToDTO(chat));
    }

    /**
     * Get specific chat details.
     * GET /api/v1/chats/{chatId}
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<ProductChatDTO> getChatDetails(@PathVariable UUID chatId) {
        ProductChat chat = productChatService.getChatById(chatId);
        return ResponseEntity.ok(mapToDTO(chat));
    }

    /**
     * Get all chats (for admin).
     * GET /api/v1/chats
     */
    @GetMapping("")
    public ResponseEntity<List<ProductChatDTO>> getAllChats(Authentication authentication) {
        // For now, return all chats (can be restricted to admin only)
        // In production, should use admin ID or similar
        // For testing, just return all chats
        List<ProductChat> allChats = productChatService.getAllChats();
        List<ProductChatDTO> dtos = allChats.stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all chats for current user (customer).
     * GET /api/v1/chats/my-chats
     */
    @GetMapping("/my-chats")
    public ResponseEntity<List<ProductChatDTO>> getMyChats(Authentication authentication) {
        UUID customerId = extractUserId(authentication);
        List<ProductChat> chats = productChatService.getCustomerChats(customerId);
        List<ProductChatDTO> dtos = chats.stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all chats for an admin.
     * GET /api/v1/chats/admin/chats
     */
    @GetMapping("/admin/chats")
    public ResponseEntity<List<ProductChatDTO>> getAdminChats(Authentication authentication) {
        UUID adminId = extractUserId(authentication);
        List<ProductChat> chats = productChatService.getAdminChats(adminId);
        List<ProductChatDTO> dtos = chats.stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Assign a chat to an admin.
     * POST /api/v1/chats/{chatId}/assign/{adminId}
     */
    @PostMapping("/{chatId}/assign/{adminId}")
    public ResponseEntity<ProductChatDTO> assignChatToAdmin(
            @PathVariable UUID chatId,
            @PathVariable UUID adminId) {

        ProductChat chat = productChatService.assignChatToAdmin(chatId, adminId);
        return ResponseEntity.ok(mapToDTO(chat));
    }

    /**
     * Close a chat.
     * POST /api/v1/chats/{chatId}/close
     */
    @PostMapping("/{chatId}/close")
    public ResponseEntity<ProductChatDTO> closeChat(@PathVariable UUID chatId) {
        ProductChat chat = productChatService.closeChat(chatId);
        return ResponseEntity.ok(mapToDTO(chat));
    }

    /**
     * Reopen a closed chat.
     * POST /api/v1/chats/{chatId}/reopen
     */
    @PostMapping("/{chatId}/reopen")
    public ResponseEntity<ProductChatDTO> reopenChat(@PathVariable UUID chatId) {
        ProductChat chat = productChatService.reopenChat(chatId);
        return ResponseEntity.ok(mapToDTO(chat));
    }

    /**
     * Send a message in a chat.
     * POST /api/v1/chats/{chatId}/messages
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable UUID chatId,
            @RequestBody SendMessageRequestDTO request,
            Authentication authentication) {

        UUID senderId = extractUserId(authentication);

        ChatMessage message;
        // Check if request has product context
        if (request.getProductContextId() != null && !request.getProductContextId().isEmpty()) {
            message = productChatService.sendMessageWithProductContext(
                    chatId,
                    senderId,
                    request.getMessage(),
                    request.getMessageType(),
                    UUID.fromString(request.getProductContextId()),
                    request.getProductContextName(),
                    request.getProductContextImage(),
                    request.getProductContextPrice()
            );
        } else {
            message = productChatService.sendMessage(
                    chatId,
                    senderId,
                    request.getMessage(),
                    request.getMessageType()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapMessageToDTO(message));
    }

    /**
     * Send a message with image URL (for client-side upload).
     * 
     * Flow:
     * 1. Client uploads image to Cloudinary directly (from Android app)
     * 2. Cloudinary returns the public URL
     * 3. Client sends this endpoint with message and imageUrl
     * 4. Server creates a chat message with the Cloudinary URL
     * 
     * POST /api/v1/chats/{chatId}/send-image-message
     */
    @PostMapping("/{chatId}/send-image-message")
    public ResponseEntity<ChatMessageDTO> sendImageMessage(
            @PathVariable UUID chatId,
            @RequestBody SendImageMessageRequestDTO request,
            Authentication authentication) {

        UUID senderId = extractUserId(authentication);

        try {
            if (request.getImageUrl() == null || request.getImageUrl().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ChatMessage message = productChatService.sendMessage(
                    chatId,
                    senderId,
                    request.getMessage() != null ? request.getMessage() : "Image",
                    "IMAGE",
                    request.getImageUrl()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(mapMessageToDTO(message));

        } catch (Exception e) {
            log.error("Failed to send image message for chat {}: {}", chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all messages in a chat.
     * GET /api/v1/chats/{chatId}/messages
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatMessages(@PathVariable UUID chatId) {
        List<ChatMessage> messages = productChatService.getChatMessages(chatId);
        List<ChatMessageDTO> dtos = messages.stream().map(this::mapMessageToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Mark a message as read.
     * PUT /api/v1/chats/messages/{messageId}/read
     */
    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ChatMessageDTO> markMessageAsRead(@PathVariable UUID messageId) {
        ChatMessage message = productChatService.markMessageAsRead(messageId);
        return ResponseEntity.ok(mapMessageToDTO(message));
    }

    /**
     * Mark all messages as read in a chat for current user.
     * PUT /api/v1/chats/{chatId}/read
     */
    @PutMapping("/{chatId}/read")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable UUID chatId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        productChatService.markAllMessagesAsRead(chatId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get unread message count in a chat.
     * GET /api/v1/chats/{chatId}/unread-count
     */
    @GetMapping("/{chatId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable UUID chatId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        long count = productChatService.getUnreadMessageCount(chatId, userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Delete a chat.
     * DELETE /api/v1/chats/{chatId}
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID chatId) {
        productChatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload image to Cloudinary and create message with image URL.
     * 
     * Flow:
     * 1. Client sends image file to this endpoint
     * 2. Server uploads image to Cloudinary
     * 3. Cloudinary returns the public URL
     * 4. Server creates a chat message with the Cloudinary URL
     * 5. Server returns the message with the image URL
     * 
     * POST /api/v1/chats/{chatId}/upload-image
     */
    @PostMapping("/{chatId}/upload-image")
    public ResponseEntity<ChatMessageDTO> uploadImage(
            @PathVariable UUID chatId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!isValidImageFile(file)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }

            // Upload to Cloudinary
            String imageUrl = cloudinaryService.uploadImage(file, "pandq/chat-images");
            
            log.info("Successfully uploaded image for chat {} to Cloudinary: {}", chatId, imageUrl);

            // Create message with Cloudinary URL (no filename in message, just empty for image-only)
            ChatMessage chatMessage = productChatService.sendMessage(
                    chatId, 
                    userId, 
                    "", 
                    "IMAGE", 
                    imageUrl
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(mapMessageToDTO(chatMessage));
            
        } catch (Exception e) {
            log.error("Failed to upload image for chat {}: {}", chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate if the uploaded file is a valid image.
     */
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        // Allow common image types
        return contentType.startsWith("image/jpeg") ||
               contentType.startsWith("image/png") ||
               contentType.startsWith("image/gif") ||
               contentType.startsWith("image/webp") ||
               contentType.startsWith("image/jpg");
    }

    /**
     * Get Cloudinary configuration for client-side unsigned upload.
     * 
     * Returns cloud_name and upload_preset needed for client-side upload.
     * This allows Android app to upload images directly to Cloudinary
     * without going through the server.
     * 
     * GET /api/v1/chats/cloudinary-config
     */
    @GetMapping("/cloudinary-config")
    public ResponseEntity<Map<String, String>> getCloudinaryConfig() {
        try {
            return ResponseEntity.ok(cloudinaryService.getCloudinaryConfig());
        } catch (Exception e) {
            log.error("Failed to get Cloudinary config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods for mapping

    private ProductChatDTO mapToDTO(ProductChat chat) {
        // Get last message if exists (sort by createdAt to get the most recent)
        String lastMessageAt = null;
        String lastMessagePreview = null;
        String lastMessageSenderRole = null;
        if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
            // Sort messages by createdAt to get the most recent one
            ChatMessage lastMessage = chat.getMessages().stream()
                    .filter(m -> m.getCreatedAt() != null)
                    .max((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                    .orElse(chat.getMessages().get(chat.getMessages().size() - 1));
            
            lastMessageAt = lastMessage.getCreatedAt() != null ? lastMessage.getCreatedAt().toString() : null;
            lastMessageSenderRole = lastMessage.getSenderRole();
            // If message is IMAGE type, show "Đã gửi 1 ảnh" instead of filename
            // Also check if imageUrl is not null/empty as a fallback
            if (lastMessage.getMessageType() == MessageType.IMAGE || 
                (lastMessage.getImageUrl() != null && !lastMessage.getImageUrl().isEmpty())) {
                lastMessagePreview = "Đã gửi 1 ảnh";
            } else {
                lastMessagePreview = lastMessage.getMessage();
            }
        }
        
        // Count unread messages FROM CUSTOMER that ADMIN hasn't read yet
        // (For admin view: count messages sent by customer that are not read)
        long unreadCount = 0;
        if (chat.getMessages() != null && chat.getCustomer() != null) {
            UUID customerId = chat.getCustomer().getId();
            unreadCount = chat.getMessages().stream()
                    .filter(m -> m.getSenderId() != null && m.getSenderId().equals(customerId) && !m.isRead())
                    .count();
        }

        return ProductChatDTO.builder()
                .id(chat.getId().toString())
                .productId(chat.getProduct() != null ? chat.getProduct().getId().toString() : null)
                .productName(chat.getProduct() != null ? chat.getProduct().getName() : "Unknown Product")
                .productImage(chat.getProduct() != null && !chat.getProduct().getImages().isEmpty() ? chat.getProduct().getImages().get(0).getImageUrl() : null)
                .productPrice(chat.getProduct() != null ? formatPrice(chat.getProduct().getPrice()) : "")
                .customerId(chat.getCustomer() != null ? chat.getCustomer().getId().toString() : null)
                .customerName(chat.getCustomer() != null ? chat.getCustomer().getFullName() : "Unknown Customer")
                .customerAvatar(chat.getCustomer() != null ? chat.getCustomer().getAvatarUrl() : null)
                .adminId(chat.getAdmin() != null ? chat.getAdmin().getId().toString() : null)
                .adminName(chat.getAdmin() != null ? chat.getAdmin().getFullName() : null)
                .subject(chat.getSubject())
                .status(chat.getStatus() != null ? chat.getStatus().toString() : "PENDING")
                .messageCount(chat.getMessages() != null ? chat.getMessages().size() : 0)
                .unreadCount(unreadCount)
                .createdAt(chat.getCreatedAt() != null ? chat.getCreatedAt().toString() : null)
                .updatedAt(chat.getUpdatedAt() != null ? chat.getUpdatedAt().toString() : null)
                .closedAt(chat.getClosedAt() != null ? chat.getClosedAt().toString() : null)
                .lastMessageAt(lastMessageAt)
                .lastMessagePreview(lastMessagePreview)
                .lastMessageSenderRole(lastMessageSenderRole)
                .build();
    }

    private ChatMessageDTO mapMessageToDTO(ChatMessage message) {
        // Use senderName and senderRole stored in the message entity
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Unknown";
        String senderRole = message.getSenderRole() != null ? message.getSenderRole() : "CUSTOMER";
        String senderAvatar = null;
        
        // Get chat ID from cached column to avoid lazy loading
        String chatId = null;
        if (message.getChatId() != null) {
            chatId = message.getChatId().toString();
        } else if (message.getProductChat() != null) {
            // Fallback to productChat if chatId cache is not available
            try {
                chatId = message.getProductChat().getId().toString();
            } catch (Exception e) {
                log.warn("Failed to get chatId for message {}", message.getId());
                chatId = "";
            }
        }
        
        if (chatId == null) {
            chatId = "";
        }
        
        return ChatMessageDTO.builder()
                .id(message.getId().toString())
                .chatId(chatId)
                .senderId(message.getSenderId().toString())
                .senderName(senderName)
                .senderRole(senderRole)
                .senderAvatar(senderAvatar)
                .message(message.getMessage())
                .messageType(message.getMessageType() != null ? message.getMessageType().toString() : "TEXT")
                .imageUrl(message.getImageUrl())
                .productContextId(message.getProductContextId() != null ? message.getProductContextId().toString() : null)
                .productContextName(message.getProductContextName())
                .productContextImage(message.getProductContextImage())
                .productContextPrice(message.getProductContextPrice())
                .isRead(message.isRead())
                .readAt(message.getReadAt() != null ? message.getReadAt().toString() : null)
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)
                .build();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null || price.signum() == 0) {
            return "";
        }
        // Format as Vietnamese currency (VND)
        long priceVND = price.longValue();
        return String.format("%,d₫", priceVND).replace(",", ".");
    }
}
