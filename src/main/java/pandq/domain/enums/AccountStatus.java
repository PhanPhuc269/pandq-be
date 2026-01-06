package pandq.domain.enums;

/**
 * Account status for user accounts.
 * Determines if a user can access the system.
 */
public enum AccountStatus {
    /**
     * Active account - normal operation
     */
    ACTIVE,

    /**
     * Inactive account - temporarily suspended
     */
    INACTIVE,

    /**
     * Banned account - permanently blocked
     */
    BANNED
}
