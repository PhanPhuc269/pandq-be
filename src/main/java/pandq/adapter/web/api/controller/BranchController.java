package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.BranchDTO;
import pandq.application.services.BranchService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<List<BranchDTO.Response>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO.Response> getBranchById(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @PostMapping
    public ResponseEntity<BranchDTO.Response> createBranch(@RequestBody BranchDTO.CreateRequest request) {
        return ResponseEntity.ok(branchService.createBranch(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDTO.Response> updateBranch(@PathVariable UUID id, @RequestBody BranchDTO.UpdateRequest request) {
        return ResponseEntity.ok(branchService.updateBranch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
