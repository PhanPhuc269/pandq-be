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
import pandq.domain.models.order.Order;
import pandq.infrastructure.persistence.repositories.jpa.JpaInventoryRepository;
import pandq.infrastructure.persistence.repositories.jpa.JpaOrderRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final JpaInventoryRepository jpaInventoryRepository;
    private final JpaOrderRepository jpaOrderRepository;

    @Transactional(readOnly = true)
    public List<InventoryDTO.Response> getInventoryByBranch(UUID branchId) {
        return inventoryRepository.findByBranchId(branchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO.Response> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryDTO.StatsResponse getInventoryStats() {
        List<Inventory> allInventory = inventoryRepository.findAll();
        
        InventoryDTO.StatsResponse stats = new InventoryDTO.StatsResponse();
        
        // Calculate total inventory value (handle null prices)
        BigDecimal totalValue = allInventory.stream()
                .filter(inv -> inv.getProduct() != null && inv.getProduct().getPrice() != null)
                .map(inv -> inv.getProduct().getPrice().multiply(BigDecimal.valueOf(inv.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalInventoryValue(totalValue);
        
        // Total products in stock
        int totalInStock = allInventory.stream()
                .mapToInt(Inventory::getQuantity)
                .sum();
        stats.setTotalProductsInStock(totalInStock);
        
        // Low stock items (quantity < 10)
        List<Inventory> lowStock = allInventory.stream()
                .filter(inv -> inv.getQuantity() < 10)
                .collect(Collectors.toList());
        stats.setLowStockCount(lowStock.size());
        stats.setLowStockItems(lowStock.stream().map(this::mapToResponse).collect(Collectors.toList()));
        
        // All items
        stats.setAllItems(allInventory.stream().map(this::mapToResponse).collect(Collectors.toList()));
        
        return stats;
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

    /**
     * Reserve inventory when order is confirmed (payment successful)
     * Increases reservedQuantity for each item in the order
     */
    @Transactional
    public void reserveInventoryForOrder(UUID productId, int quantity) {
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (!inventories.isEmpty()) {
            Inventory inventory = inventories.get(0);
            int currentReserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
            inventory.setReservedQuantity(currentReserved + quantity);
            inventoryRepository.save(inventory);
        }
    }

    /**
     * Complete order inventory - when order is delivered/completed
     * Decreases both quantity (actual stock) and reservedQuantity
     */
    @Transactional
    public void completeOrderInventory(UUID productId, int quantity) {
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (!inventories.isEmpty()) {
            Inventory inventory = inventories.get(0);
            
            // Decrease actual stock
            int currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            inventory.setQuantity(Math.max(0, currentQuantity - quantity));
            
            // Decrease reserved quantity
            int currentReserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
            inventory.setReservedQuantity(Math.max(0, currentReserved - quantity));
            
            inventoryRepository.save(inventory);
        }
    }

    /**
     * Cancel order inventory - when order is cancelled
     * Only decreases reservedQuantity (returns items to available stock)
     */
    @Transactional
    public void cancelOrderInventory(UUID productId, int quantity) {
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (!inventories.isEmpty()) {
            Inventory inventory = inventories.get(0);
            int currentReserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
            inventory.setReservedQuantity(Math.max(0, currentReserved - quantity));
            inventoryRepository.save(inventory);
        }
    }

    /**
     * Recalculate and sync reserved quantities based on actual active orders.
     * This is useful when data becomes inconsistent due to direct DB updates
     * or seed data that bypasses the order status change logic.
     * 
     * Active orders = CONFIRMED or SHIPPING status (orders that have inventory reserved)
     * 
     * @return Number of inventory items that were corrected
     */
    @Transactional
    public int recalculateReservedQuantities() {
        // Step 1: Reset all reserved quantities to 0
        jpaInventoryRepository.resetAllReservedQuantities();
        
        // Step 2: Get all active orders (CONFIRMED or SHIPPING)
        List<Order> activeOrders = jpaOrderRepository.findActiveOrdersWithReservedInventory();
        
        // Step 3: Calculate total reserved quantity per product
        Map<UUID, Integer> productReservedMap = new HashMap<>();
        for (Order order : activeOrders) {
            for (var item : order.getOrderItems()) {
                UUID productId = item.getProduct().getId();
                int quantity = item.getQuantity();
                productReservedMap.merge(productId, quantity, Integer::sum);
            }
        }
        
        // Step 4: Update reserved quantities for products with active orders
        int correctedCount = 0;
        for (Map.Entry<UUID, Integer> entry : productReservedMap.entrySet()) {
            jpaInventoryRepository.updateReservedQuantityByProductId(entry.getKey(), entry.getValue());
            correctedCount++;
        }
        
        return correctedCount;
    }

    private InventoryDTO.Response mapToResponse(Inventory inventory) {
        InventoryDTO.Response response = new InventoryDTO.Response();
        response.setId(inventory.getId());
        
        if (inventory.getBranch() != null) {
            response.setBranchId(inventory.getBranch().getId());
            response.setBranchName(inventory.getBranch().getName());
        }
        
        if (inventory.getProduct() != null) {
            response.setProductId(inventory.getProduct().getId());
            response.setProductName(inventory.getProduct().getName());
            response.setProductThumbnail(inventory.getProduct().getThumbnailUrl());
            response.setProductSku("SKU-" + inventory.getProduct().getId().toString().substring(0, 8).toUpperCase());
            response.setProductPrice(inventory.getProduct().getPrice());
        }
        
        response.setQuantity(inventory.getQuantity());
        response.setMinStock(inventory.getMinStock());
        response.setReservedQuantity(inventory.getReservedQuantity());
        return response;
    }
}
