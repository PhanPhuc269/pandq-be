package pandq.application.port.repositories;

import pandq.domain.models.user.Address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {
    Address save(Address address);
    Optional<Address> findById(UUID id);
    List<Address> findByUserId(UUID userId);
    void deleteById(UUID id);
}
