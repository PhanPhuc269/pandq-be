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
}
