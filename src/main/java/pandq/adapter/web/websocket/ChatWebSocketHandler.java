package pandq.adapter.web.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import pandq.adapter.web.websocket.dto.WebSocketChatMessage;
import pandq.application.services.ProductChatService;
import pandq.domain.models.chat.MessageType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat communication between customers and admins.
 * Manages chat connections and broadcasts messages to relevant parties.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ProductChatService productChatService;
    private final ObjectMapper objectMapper;

    // Map of chat sessions: chatId -> Set of WebSocketSessions
    private static final Map<String, Set<WebSocketSession>> chatSessions = new ConcurrentHashMap<>();

    // Map to track which chat each session is connected to: sessionId -> chatId
    private static final Map<String, String> sessionChatMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        // Client will send initial message with chatId
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            WebSocketChatMessage chatMessage = objectMapper.readValue(payload, WebSocketChatMessage.class);

            String chatId = chatMessage.getChatId();
            String senderId = chatMessage.getSenderId();

            // Register session to chat if not already registered
            if (!sessionChatMap.containsKey(session.getId())) {
                sessionChatMap.put(session.getId(), chatId);
                chatSessions.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(session);
                log.info("Session {} joined chat {}", session.getId(), chatId);
            }

            // Save message to database
            productChatService.sendMessage(
                    UUID.fromString(chatId),
                    UUID.fromString(senderId),
                    chatMessage.getMessage(),
                    MessageType.TEXT
            );

            // Broadcast message to all connected clients in this chat
            broadcastToChat(chatId, chatMessage);

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            session.sendMessage(new TextMessage(createErrorMessage("Failed to process message")));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String chatId = sessionChatMap.remove(session.getId());
        if (chatId != null) {
            Set<WebSocketSession> sessions = chatSessions.get(chatId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    chatSessions.remove(chatId);
                }
            }
            log.info("Session {} left chat {}", session.getId(), chatId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    /**
     * Broadcast a message to all connected clients in a specific chat.
     */
    private void broadcastToChat(String chatId, WebSocketChatMessage message) {
        Set<WebSocketSession> sessions = chatSessions.get(chatId);
        if (sessions != null && !sessions.isEmpty()) {
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(messageJson);

                sessions.stream()
                        .filter(WebSocketSession::isOpen)
                        .forEach(session -> {
                            try {
                                session.sendMessage(textMessage);
                            } catch (IOException e) {
                                log.warn("Failed to send message to session {}", session.getId(), e);
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to broadcast message to chat {}", chatId, e);
            }
        }
    }

    /**
     * Send a message to a specific chat.
     */
    public void sendToChatId(String chatId, WebSocketChatMessage message) {
        broadcastToChat(chatId, message);
    }

    /**
     * Get number of connected users in a chat.
     */
    public int getConnectedUsersCount(String chatId) {
        Set<WebSocketSession> sessions = chatSessions.get(chatId);
        return sessions != null ? (int) sessions.stream().filter(WebSocketSession::isOpen).count() : 0;
    }

    /**
     * Check if a chat has any connected users.
     */
    public boolean hasChatActive(String chatId) {
        return getConnectedUsersCount(chatId) > 0;
    }

    /**
     * Create error message JSON.
     */
    private String createErrorMessage(String error) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "ERROR",
                    "message", error,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return "{\"type\":\"ERROR\",\"message\":\"" + error + "\"}";
        }
    }
}
