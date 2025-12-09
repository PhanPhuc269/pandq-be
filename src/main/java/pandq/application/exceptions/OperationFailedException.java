package pandq.application.exceptions;

public class OperationFailedException extends ApplicationException {
    public OperationFailedException(String operation, String reason) {
        super(operation + " failed: " + reason);
    }
}
