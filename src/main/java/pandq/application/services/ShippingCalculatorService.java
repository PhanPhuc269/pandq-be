package pandq.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pandq.adapter.web.api.dtos.ShippingDTO;
import pandq.domain.models.order.OrderItem;
import pandq.domain.models.shipping.ShippingZone;
import pandq.infrastructure.persistence.repositories.jpa.JpaShippingZoneRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingCalculatorService {

    private final JpaShippingZoneRepository shippingZoneRepository;
    private final ObjectMapper objectMapper;

    // Default values if no zone config found
    private static final BigDecimal DEFAULT_BASE_FEE = new BigDecimal("25000");
    private static final BigDecimal DEFAULT_FREE_SHIP_THRESHOLD = new BigDecimal("500000");

    /**
     * Calculate shipping fee for a list of order items (during checkout)
     * Simplified for electronics: flat rate by zone only
     */
    public ShippingDTO.CalculateResponse calculateFromOrderItems(
            String shippingAddress,
            List<OrderItem> orderItems,
            BigDecimal totalAmount
    ) {
        // Determine zone from address
        ShippingZone zone = determineZoneFromAddress(shippingAddress);
        return calculateFee(zone, totalAmount);
    }

    /**
     * Calculate shipping fee for cart preview (before checkout)
     */
    public ShippingDTO.CalculateResponse calculateForPreview(ShippingDTO.CalculateRequest request) {
        // Determine zone from city/district
        ShippingZone zone = determineZoneFromCityDistrict(request.getCity(), request.getDistrict());
        return calculateFee(zone, request.getTotalAmount());
    }

    /**
     * Core calculation logic - flat rate by zone
     */
    private ShippingDTO.CalculateResponse calculateFee(ShippingZone zone, BigDecimal totalAmount) {
        BigDecimal baseFee = zone != null ? zone.getBaseFee() : DEFAULT_BASE_FEE;
        BigDecimal freeShipThreshold = zone != null && zone.getFreeShipThreshold() != null 
                ? zone.getFreeShipThreshold() 
                : DEFAULT_FREE_SHIP_THRESHOLD;
        
        // Check free shipping eligibility
        boolean isFreeShip = totalAmount != null && totalAmount.compareTo(freeShipThreshold) >= 0;
        
        BigDecimal shippingFee;
        String freeShipReason = null;
        
        if (isFreeShip) {
            shippingFee = BigDecimal.ZERO;
            freeShipReason = "Miễn phí vận chuyển cho đơn hàng từ " + formatCurrency(freeShipThreshold);
        } else {
            // Flat rate based on zone
            shippingFee = baseFee;
        }
        
        // Calculate amount needed for free ship
        BigDecimal amountToFreeShip = BigDecimal.ZERO;
        if (totalAmount != null && !isFreeShip) {
            amountToFreeShip = freeShipThreshold.subtract(totalAmount);
            if (amountToFreeShip.compareTo(BigDecimal.ZERO) < 0) {
                amountToFreeShip = BigDecimal.ZERO;
            }
        }
        
        return ShippingDTO.CalculateResponse.builder()
                .shippingFee(shippingFee)
                .zoneName(zone != null ? zone.getName() : "Mặc định")
                .zoneLevel(zone != null ? zone.getZoneLevel() : 2)
                .chargeableWeight(null)  // Not applicable for electronics
                .isFreeShip(isFreeShip)
                .freeShipReason(freeShipReason)
                .freeShipThreshold(freeShipThreshold)
                .amountToFreeShip(amountToFreeShip)
                .build();
    }

    /**
     * Determine shipping zone from full address JSON
     */
    private ShippingZone determineZoneFromAddress(String shippingAddress) {
        if (shippingAddress == null || shippingAddress.isEmpty()) {
            return getDefaultZone();
        }
        
        try {
            // Try to parse as JSON
            JsonNode addressNode = objectMapper.readTree(shippingAddress);
            String city = addressNode.has("city") ? addressNode.get("city").asText() : null;
            String district = addressNode.has("district") ? addressNode.get("district").asText() : null;
            
            return determineZoneFromCityDistrict(city, district);
        } catch (Exception e) {
            log.warn("Could not parse shipping address: {}", shippingAddress);
            // Try to extract city from plain text
            return determineZoneFromPlainAddress(shippingAddress);
        }
    }

    /**
     * Determine zone from city and district
     */
    private ShippingZone determineZoneFromCityDistrict(String city, String district) {
        if (city == null) {
            return getDefaultZone();
        }
        
        List<ShippingZone> zones = shippingZoneRepository.findByIsActiveTrueOrderByZoneLevelAsc();
        
        for (ShippingZone zone : zones) {
            if (zone.getCities() != null) {
                String citiesLower = zone.getCities().toLowerCase();
                if (citiesLower.contains(city.toLowerCase())) {
                    // Check if district matches (for inner-city vs outer-city)
                    if (zone.getDistricts() != null && district != null) {
                        String districtsLower = zone.getDistricts().toLowerCase();
                        if (districtsLower.contains(district.toLowerCase())) {
                            return zone;
                        }
                    } else {
                        return zone;
                    }
                }
            }
        }
        
        // Return highest level zone (remote areas) as fallback
        return zones.isEmpty() ? getDefaultZone() : zones.get(zones.size() - 1);
    }

    /**
     * Try to extract city from plain text address
     */
    private ShippingZone determineZoneFromPlainAddress(String address) {
        String addressLower = address.toLowerCase();
        
        List<ShippingZone> zones = shippingZoneRepository.findByIsActiveTrueOrderByZoneLevelAsc();
        
        for (ShippingZone zone : zones) {
            if (zone.getCities() != null) {
                // Parse cities JSON and check each
                try {
                    JsonNode citiesArray = objectMapper.readTree(zone.getCities());
                    for (JsonNode cityNode : citiesArray) {
                        String cityName = cityNode.asText().toLowerCase();
                        if (addressLower.contains(cityName)) {
                            return zone;
                        }
                    }
                } catch (Exception e) {
                    // If not JSON, try plain string match
                    if (addressLower.contains(zone.getCities().toLowerCase())) {
                        return zone;
                    }
                }
            }
        }
        
        return getDefaultZone();
    }

    /**
     * Get default zone for fallback
     */
    private ShippingZone getDefaultZone() {
        List<ShippingZone> zones = shippingZoneRepository.findByIsActiveTrueOrderByZoneLevelAsc();
        if (!zones.isEmpty()) {
            // Return level 2 (suburban) as default
            for (ShippingZone zone : zones) {
                if (zone.getZoneLevel() != null && zone.getZoneLevel() == 2) {
                    return zone;
                }
            }
            return zones.get(0);
        }
        return null;
    }

    /**
     * Get all available shipping zones
     */
    public List<ShippingDTO.ZoneResponse> getAllZones() {
        return shippingZoneRepository.findByIsActiveTrue().stream()
                .map(zone -> ShippingDTO.ZoneResponse.builder()
                        .id(zone.getId())
                        .name(zone.getName())
                        .zoneLevel(zone.getZoneLevel())
                        .baseFee(zone.getBaseFee())
                        .freeShipThreshold(zone.getFreeShipThreshold())
                        .build())
                .toList();
    }

    /**
     * Format currency for display
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0fđ", amount);
    }
}
