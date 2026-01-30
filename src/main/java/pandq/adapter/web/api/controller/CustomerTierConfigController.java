package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.CustomerTierConfigDTO;
import pandq.application.services.CustomerTierConfigService;
import pandq.domain.enums.CustomerTier;

/**
 * Controller for managing customer tier configurations.
 * Allows admin to view and modify tier thresholds.
 */
@RestController
@RequestMapping("/api/v1/customer-tier-config")
@RequiredArgsConstructor
@Slf4j
public class CustomerTierConfigController {

    private final CustomerTierConfigService configService;

    /**
     * Get all tier configurations
     * 
     * @return List of all tier configurations
     */
    @GetMapping
    public ResponseEntity<CustomerTierConfigDTO.TierConfigListResponse> getAllConfigs() {
        log.info("GET /api/v1/customer-tier-config");

        CustomerTierConfigDTO.TierConfigListResponse response = configService.getAllConfigs();
        return ResponseEntity.ok(response);
    }

    /**
     * Get configuration for a specific tier
     * 
     * @param tier The customer tier
     * @return Tier configuration
     */
    @GetMapping("/{tier}")
    public ResponseEntity<CustomerTierConfigDTO.TierConfigDto> getConfigByTier(
            @PathVariable CustomerTier tier) {
        log.info("GET /api/v1/customer-tier-config/{}", tier);

        CustomerTierConfigDTO.TierConfigDto config = configService.getConfigByTier(tier);
        return ResponseEntity.ok(config);
    }

    /**
     * Update tier configurations
     * 
     * @param request List of tier configurations to update
     * @return Success response
     */
    @PutMapping
    public ResponseEntity<Void> updateConfigs(
            @RequestBody CustomerTierConfigDTO.UpdateAllTierConfigsRequest request) {
        log.info("PUT /api/v1/customer-tier-config - updating {} configs",
                request.getConfigs() != null ? request.getConfigs().size() : 0);

        // TODO: Get actual user from security context
        String updatedBy = "admin";

        configService.updateConfigs(request, updatedBy);
        return ResponseEntity.ok().build();
    }
}
