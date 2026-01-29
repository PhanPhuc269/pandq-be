package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.CustomerTierConfigDTO;
import pandq.application.port.repositories.CustomerTierConfigRepository;
import pandq.domain.enums.CustomerTier;
import pandq.domain.models.config.CustomerTierConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing customer tier configurations.
 * Allows admin to modify spending thresholds for each tier.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerTierConfigService {

    private final CustomerTierConfigRepository repository;

    /**
     * Get all tier configurations
     */
    @Transactional(readOnly = true)
    public CustomerTierConfigDTO.TierConfigListResponse getAllConfigs() {
        log.info("Getting all customer tier configurations");

        List<CustomerTierConfig> configs = repository.findAll();

        List<CustomerTierConfigDTO.TierConfigDto> dtos = configs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return CustomerTierConfigDTO.TierConfigListResponse.builder()
                .configs(dtos)
                .build();
    }

    /**
     * Update tier configurations
     */
    @Transactional
    public void updateConfigs(CustomerTierConfigDTO.UpdateAllTierConfigsRequest request, String updatedBy) {
        log.info("Updating tier configurations by: {}", updatedBy);

        for (CustomerTierConfigDTO.UpdateTierConfigRequest configRequest : request.getConfigs()) {
            CustomerTierConfig config = repository.findByTier(configRequest.getTier())
                    .orElseThrow(() -> new RuntimeException("Tier config not found: " + configRequest.getTier()));

            config.setMinSpent(configRequest.getMinSpent());
            config.setMaxSpent(configRequest.getMaxSpent());

            if (configRequest.getDisplayName() != null) {
                config.setDisplayName(configRequest.getDisplayName());
            }
            if (configRequest.getDescription() != null) {
                config.setDescription(configRequest.getDescription());
            }

            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(updatedBy);

            repository.save(config);
            log.info("Updated tier config: {} with minSpent: {}, maxSpent: {}",
                    config.getTier(), config.getMinSpent(), config.getMaxSpent());
        }
    }

    /**
     * Determine customer tier based on total spent amount.
     * Uses database configuration instead of hardcoded values.
     */
    @Transactional(readOnly = true)
    public CustomerTier getTierFromSpent(BigDecimal totalSpent) {
        if (totalSpent == null) {
            return CustomerTier.BRONZE;
        }

        List<CustomerTierConfig> configs = repository.findAllByIsActiveTrue();

        // Sort by minSpent descending to check highest tier first
        configs.sort((a, b) -> b.getMinSpent().compareTo(a.getMinSpent()));

        for (CustomerTierConfig config : configs) {
            if (totalSpent.compareTo(config.getMinSpent()) >= 0) {
                // Check maxSpent if exists
                if (config.getMaxSpent() == null || totalSpent.compareTo(config.getMaxSpent()) <= 0) {
                    return config.getTier();
                }
            }
        }

        // Default to BRONZE if no match
        return CustomerTier.BRONZE;
    }

    /**
     * Get configuration for a specific tier
     */
    @Transactional(readOnly = true)
    public CustomerTierConfigDTO.TierConfigDto getConfigByTier(CustomerTier tier) {
        CustomerTierConfig config = repository.findByTier(tier)
                .orElseThrow(() -> new RuntimeException("Tier config not found: " + tier));
        return toDto(config);
    }

    private CustomerTierConfigDTO.TierConfigDto toDto(CustomerTierConfig config) {
        return CustomerTierConfigDTO.TierConfigDto.builder()
                .id(config.getId().toString())
                .tier(config.getTier())
                .minSpent(config.getMinSpent())
                .maxSpent(config.getMaxSpent())
                .displayName(config.getDisplayName())
                .description(config.getDescription())
                .isActive(config.getIsActive())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
