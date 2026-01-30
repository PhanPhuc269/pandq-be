package pandq.application.port.repositories;

import pandq.domain.enums.CustomerTier;
import pandq.domain.models.config.CustomerTierConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CustomerTierConfig.
 */
public interface CustomerTierConfigRepository {

    List<CustomerTierConfig> findAll();

    Optional<CustomerTierConfig> findByTier(CustomerTier tier);

    CustomerTierConfig save(CustomerTierConfig config);

    List<CustomerTierConfig> saveAll(List<CustomerTierConfig> configs);

    List<CustomerTierConfig> findAllByIsActiveTrue();
}
