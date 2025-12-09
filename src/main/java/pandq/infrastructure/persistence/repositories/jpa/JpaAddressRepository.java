package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.user.Address;

import java.util.List;
import java.util.UUID;

public interface JpaAddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserId(UUID userId);
}
