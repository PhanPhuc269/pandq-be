package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pandq.domain.enums.CustomerTier;
import pandq.domain.models.config.CustomerTierConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCustomerTierConfigRepository extends JpaRepository<CustomerTierConfig, UUID> {

    Optional<CustomerTierConfig> findByTier(CustomerTier tier);

    List<CustomerTierConfig> findAllByIsActiveTrue();

    List<CustomerTierConfig> findAllByOrderByMinSpentAsc();
}
