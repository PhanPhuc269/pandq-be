package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.AddressRepository;
import pandq.domain.models.user.Address;
import pandq.infrastructure.persistence.repositories.jpa.JpaAddressRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {

    private final JpaAddressRepository jpaAddressRepository;

    @Override
    public Address save(Address address) {
        return jpaAddressRepository.save(address);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return jpaAddressRepository.findById(id);
    }

    @Override
    public List<Address> findByUserId(UUID userId) {
        return jpaAddressRepository.findByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaAddressRepository.deleteById(id);
    }
}
