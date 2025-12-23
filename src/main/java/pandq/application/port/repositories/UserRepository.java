package pandq.application.port.repositories;


import pandq.domain.models.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);
    Optional<User> findById(UUID id);
    List<User> findAll();
    Optional<User> findByEmail(String email);
    Optional<User> findByFirebaseUid(String firebaseUid);
    void deleteById(UUID id);
}

