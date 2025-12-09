package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.branch.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByBranchId(UUID branchId);
    List<Inventory> findByProductId(UUID productId);
    Optional<Inventory> findByBranchIdAndProductId(UUID branchId, UUID productId);
}
