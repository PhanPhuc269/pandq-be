package pandq.application.port.repositories;

import pandq.domain.models.shipping.ShippingZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingZoneRepository {
    
    List<ShippingZone> findAll();
    
    Optional<ShippingZone> findById(UUID id);
    
    List<ShippingZone> findByIsActiveTrue();
    
    Optional<ShippingZone> findByName(String name);
    
    /**
     * Find zone by city name (checks if city is in the cities JSON array)
     */
    List<ShippingZone> findByIsActiveTrueOrderByZoneLevelAsc();
    
    ShippingZone save(ShippingZone zone);
    
    void deleteById(UUID id);
}
