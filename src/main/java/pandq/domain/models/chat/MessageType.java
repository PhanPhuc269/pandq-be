package pandq.domain.models.chat;

/**
 * Represents the type of message in a chat.
 */
public enum MessageType {
    TEXT,      // Plain text message
    IMAGE,     // Image message
    FILE,      // File attachment
    SYSTEM     // System message (e.g., "Chat closed", "Admin joined")
}
