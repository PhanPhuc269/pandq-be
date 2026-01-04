package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pandq.adapter.web.api.dtos.AppConfigDTO;
import pandq.application.services.CategoryService;
import pandq.application.services.BranchService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AppConfigController {

    private final CategoryService categoryService;
    private final BranchService branchService;

    /**
     * Returns version numbers for locations and categories.
     * Used by app to check if local cache needs refresh.
     */
    @GetMapping("/init-config")
    public ResponseEntity<AppConfigDTO.InitConfigResponse> getInitConfig() {
        // Version numbers - can be stored in database or config
        // For now, using static versions
        return ResponseEntity.ok(
                AppConfigDTO.InitConfigResponse.builder()
                        .locationVersion(1)
                        .categoryVersion(1)
                        .build());
    }

    /**
     * Returns list of store locations (branches).
     */
    @GetMapping("/master-data/locations")
    public ResponseEntity<List<AppConfigDTO.LocationResponse>> getLocations() {
        List<AppConfigDTO.LocationResponse> locations = branchService.getAllBranches().stream()
                .map(branch -> AppConfigDTO.LocationResponse.builder()
                        .id(branch.getId().toString())
                        .name(branch.getName())
                        .address(branch.getAddress())
                        .latitude(branch.getLatitude())
                        .longitude(branch.getLongitude())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(locations);
    }

    /**
     * Returns list of categories with icons.
     */
    @GetMapping("/master-data/categories")
    public ResponseEntity<List<AppConfigDTO.CategoryResponse>> getCategories() {
        List<AppConfigDTO.CategoryResponse> categories = categoryService.getAllCategories().stream()
                .map(category -> AppConfigDTO.CategoryResponse.builder()
                        .id(category.getId().toString())
                        .name(category.getName())
                        .imageUrl(category.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }
}
