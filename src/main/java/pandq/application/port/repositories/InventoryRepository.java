package pandq.application.port.repositories;

import pandq.domain.models.branch.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findById(UUID id);
    Optional<Inventory> findByBranchIdAndProductId(UUID branchId, UUID productId);
    List<Inventory> findByBranchId(UUID branchId);
    List<Inventory> findByProductId(UUID productId);
    void deleteById(UUID id);
}
