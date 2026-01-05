package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pandq.adapter.web.api.dtos.AdminDashboardDTO;
import pandq.application.services.AdminDashboardService;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardDTO.SummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}
