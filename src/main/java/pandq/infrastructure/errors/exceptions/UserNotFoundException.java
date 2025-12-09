package pandq.infrastructure.errors.exceptions;

import pandq.application.exceptions.OperationFailedException;

public class UserNotFoundException extends OperationFailedException {
    public UserNotFoundException(String message) {
        super(message, null);
    }
}
