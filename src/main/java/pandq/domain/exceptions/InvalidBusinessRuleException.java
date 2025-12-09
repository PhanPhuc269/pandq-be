package pandq.domain.exceptions;

public class InvalidBusinessRuleException extends RuntimeException {
    public InvalidBusinessRuleException(String message) {
        super(message);
    }
}
