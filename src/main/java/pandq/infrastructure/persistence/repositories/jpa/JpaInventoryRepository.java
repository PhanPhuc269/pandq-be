package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pandq.domain.models.branch.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByBranchId(UUID branchId);
    List<Inventory> findByProductId(UUID productId);
    Optional<Inventory> findByBranchIdAndProductId(UUID branchId, UUID productId);
    
    /**
     * Reset all reserved quantities to 0
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = 0")
    void resetAllReservedQuantities();
    
    /**
     * Update reserved quantity for a specific product
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = :quantity WHERE i.product.id = :productId")
    void updateReservedQuantityByProductId(@Param("productId") UUID productId, @Param("quantity") int quantity);
}
