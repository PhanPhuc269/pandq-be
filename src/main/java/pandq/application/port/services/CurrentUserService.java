package pandq.application.port.services;

import java.util.UUID;
import pandq.domain.models.user.User;

public interface CurrentUserService {
    User getCurrentUser();
    UUID getCurrentUserId();
}
