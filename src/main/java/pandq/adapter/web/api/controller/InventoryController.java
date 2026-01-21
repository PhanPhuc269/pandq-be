package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.InventoryDTO;
import pandq.application.services.InventoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/stats")
    public ResponseEntity<InventoryDTO.StatsResponse> getInventoryStats() {
        return ResponseEntity.ok(inventoryService.getInventoryStats());
    }

    @GetMapping
    public ResponseEntity<List<InventoryDTO.Response>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<InventoryDTO.Response>> getInventoryByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(inventoryService.getInventoryByBranch(branchId));
    }

    @PutMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<InventoryDTO.Response> updateInventory(
            @PathVariable UUID branchId,
            @PathVariable UUID productId,
            @RequestBody InventoryDTO.UpdateRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(branchId, productId, request));
    }
    
    /**
     * Recalculate and sync reserved quantities based on actual active orders.
     * Use this endpoint when reserved quantities are out of sync with order statuses.
     * Active orders = CONFIRMED or SHIPPING status
     * 
     * @return Number of inventory items that were corrected
     */
    @PostMapping("/recalculate-reserved")
    public ResponseEntity<String> recalculateReservedQuantities() {
        int correctedCount = inventoryService.recalculateReservedQuantities();
        return ResponseEntity.ok("Recalculated reserved quantities. Products with active reservations: " + correctedCount);
    }
}
