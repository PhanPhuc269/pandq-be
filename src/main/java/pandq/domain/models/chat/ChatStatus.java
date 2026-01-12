package pandq.domain.models.chat;

/**
 * Represents the status of a product chat conversation.
 */
public enum ChatStatus {
    OPEN,      // Chat is active
    CLOSED,    // Chat has been closed
    PENDING    // Waiting for admin response
}
