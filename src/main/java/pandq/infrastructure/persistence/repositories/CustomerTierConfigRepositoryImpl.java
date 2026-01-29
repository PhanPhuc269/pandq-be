package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.CustomerTierConfigRepository;
import pandq.domain.enums.CustomerTier;
import pandq.domain.models.config.CustomerTierConfig;
import pandq.infrastructure.persistence.repositories.jpa.JpaCustomerTierConfigRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomerTierConfigRepositoryImpl implements CustomerTierConfigRepository {

    private final JpaCustomerTierConfigRepository jpaRepository;

    @Override
    public List<CustomerTierConfig> findAll() {
        return jpaRepository.findAllByOrderByMinSpentAsc();
    }

    @Override
    public Optional<CustomerTierConfig> findByTier(CustomerTier tier) {
        return jpaRepository.findByTier(tier);
    }

    @Override
    public CustomerTierConfig save(CustomerTierConfig config) {
        return jpaRepository.save(config);
    }

    @Override
    public List<CustomerTierConfig> saveAll(List<CustomerTierConfig> configs) {
        return jpaRepository.saveAll(configs);
    }

    @Override
    public List<CustomerTierConfig> findAllByIsActiveTrue() {
        return jpaRepository.findAllByIsActiveTrue();
    }
}
