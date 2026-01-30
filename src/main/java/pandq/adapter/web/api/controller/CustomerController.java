package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.CustomerDTO;
import pandq.application.services.CustomerService;
import pandq.domain.enums.AccountStatus;
import pandq.domain.enums.CustomerTier;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Get paginated customer list with optional search and filters
     * 
     * @param page   Page number (0-indexed)
     * @param size   Page size
     * @param search Search term for name/email/phone
     * @param tier   Filter by customer tier
     * @param status Filter by account status
     * @return Paginated customer list
     */
    @GetMapping
    public ResponseEntity<CustomerDTO.CustomerListResponse> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerTier tier,
            @RequestParam(required = false) AccountStatus status) {
        log.info("GET /api/v1/customers - page: {}, size: {}, search: {}, tier: {}, status: {}",
                page, size, search, tier, status);

        CustomerDTO.CustomerListResponse response = customerService.getCustomers(page, size, search, tier, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed customer information
     * 
     * @param id Customer UUID
     * @return Customer detail with order history
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO.CustomerDetailDto> getCustomerDetail(@PathVariable UUID id) {
        log.info("GET /api/v1/customers/{}", id);

        CustomerDTO.CustomerDetailDto customer = customerService.getCustomerDetail(id);
        return ResponseEntity.ok(customer);
    }

    /**
     * Update customer account status
     * 
     * @param id      Customer UUID
     * @param request Status update request
     * @return Success response
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateCustomerStatus(
            @PathVariable UUID id,
            @RequestBody CustomerDTO.UpdateStatusRequest request) {
        log.info("PUT /api/v1/customers/{}/status - new status: {}", id, request.getStatus());

        customerService.updateCustomerStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    /**
     * Get customer statistics and tier distribution
     * 
     * @return Customer stats
     */
    @GetMapping("/stats")
    public ResponseEntity<CustomerDTO.CustomerStatsDto> getCustomerStats() {
        log.info("GET /api/v1/customers/stats");

        CustomerDTO.CustomerStatsDto stats = customerService.getCustomerStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Recalculate and update customer spending and tier
     * For admin manual recalculation
     * 
     * @param id Customer UUID
     * @return Success response
     */
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<Void> recalculateCustomerTier(@PathVariable UUID id) {
        log.info("POST /api/v1/customers/{}/recalculate", id);

        customerService.updateCustomerSpendingAndTier(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Recalculate spending and tier for ALL customers
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> syncAllCustomerSpending() {
        log.info("POST /api/v1/customers/sync");
        customerService.syncAllCustomerSpending();
        return ResponseEntity.ok().build();
    }
}
