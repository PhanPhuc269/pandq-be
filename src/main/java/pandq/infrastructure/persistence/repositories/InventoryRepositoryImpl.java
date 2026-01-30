package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.InventoryRepository;
import pandq.domain.models.branch.Inventory;
import pandq.infrastructure.persistence.repositories.jpa.JpaInventoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final JpaInventoryRepository jpaInventoryRepository;

    @Override
    public Inventory save(Inventory inventory) {
        return jpaInventoryRepository.save(inventory);
    }

    @Override
    public Optional<Inventory> findById(UUID id) {
        return jpaInventoryRepository.findById(id);
    }

    @Override
    public Optional<Inventory> findByBranchIdAndProductId(UUID branchId, UUID productId) {
        return jpaInventoryRepository.findByBranchIdAndProductId(branchId, productId);
    }

    @Override
    public List<Inventory> findByBranchId(UUID branchId) {
        return jpaInventoryRepository.findByBranchId(branchId);
    }

    @Override
    public List<Inventory> findByProductId(UUID productId) {
        return jpaInventoryRepository.findByProductId(productId);
    }

    @Override
    public List<Inventory> findAll() {
        return jpaInventoryRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaInventoryRepository.deleteById(id);
    }
}
