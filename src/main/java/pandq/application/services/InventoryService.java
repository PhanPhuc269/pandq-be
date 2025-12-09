package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.InventoryDTO;
import pandq.application.port.repositories.BranchRepository;
import pandq.application.port.repositories.InventoryRepository;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.branch.Branch;
import pandq.domain.models.branch.Inventory;
import pandq.domain.models.product.Product;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<InventoryDTO.Response> getInventoryByBranch(UUID branchId) {
        return inventoryRepository.findByBranchId(branchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryDTO.Response updateInventory(UUID branchId, UUID productId, InventoryDTO.UpdateRequest request) {
        Inventory inventory = inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseGet(() -> createNewInventory(branchId, productId));

        inventory.setQuantity(request.getQuantity());
        if (request.getMinStock() != null) {
            inventory.setMinStock(request.getMinStock());
        }

        Inventory savedInventory = inventoryRepository.save(inventory);
        return mapToResponse(savedInventory);
    }

    private Inventory createNewInventory(UUID branchId, UUID productId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return Inventory.builder()
                .branch(branch)
                .product(product)
                .quantity(0)
                .reservedQuantity(0)
                .minStock(0)
                .build();
    }

    private InventoryDTO.Response mapToResponse(Inventory inventory) {
        InventoryDTO.Response response = new InventoryDTO.Response();
        response.setId(inventory.getId());
        response.setBranchId(inventory.getBranch().getId());
        response.setBranchName(inventory.getBranch().getName());
        response.setProductId(inventory.getProduct().getId());
        response.setProductName(inventory.getProduct().getName());
        response.setQuantity(inventory.getQuantity());
        response.setMinStock(inventory.getMinStock());
        response.setReservedQuantity(inventory.getReservedQuantity());
        return response;
    }
}
