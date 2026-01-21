package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.ShippingZoneRepository;
import pandq.domain.models.shipping.ShippingZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaShippingZoneRepository extends JpaRepository<ShippingZone, UUID>, ShippingZoneRepository {
    
    @Override
    List<ShippingZone> findByIsActiveTrue();
    
    @Override
    Optional<ShippingZone> findByName(String name);
    
    @Override
    List<ShippingZone> findByIsActiveTrueOrderByZoneLevelAsc();
}
