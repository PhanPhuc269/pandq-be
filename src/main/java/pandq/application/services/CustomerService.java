package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.CustomerDTO;
import pandq.application.port.repositories.OrderRepository;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.enums.AccountStatus;
import pandq.domain.enums.CustomerTier;
import pandq.domain.models.order.Order;
import pandq.domain.models.user.User;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

        private final UserRepository userRepository;
        private final OrderRepository orderRepository;

        /**
         * Get paginated customer list with search and filters
         */
        @Transactional(readOnly = true)
        public CustomerDTO.CustomerListResponse getCustomers(
                        int page,
                        int size,
                        String search,
                        CustomerTier tierFilter,
                        AccountStatus statusFilter) {
                log.info("Getting customers - page: {}, size: {}, search: {}, tier: {}, status: {}",
                                page, size, search, tierFilter, statusFilter);

                // Get all users
                List<User> allUsers = userRepository.findAll();

                // Build list of DTOs first (with calculated tier), then filter
                List<CustomerDTO.CustomerListItemDto> allDtos = allUsers.stream()
                                .filter(u -> search == null || search.trim().isEmpty() || matchesSearch(u, search))
                                .filter(u -> statusFilter == null || u.getAccountStatus() == statusFilter)
                                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                                .map(this::toListItemDto)
                                .collect(Collectors.toList());

                // Apply tier filter on calculated tier (from DTO)
                List<CustomerDTO.CustomerListItemDto> filtered = allDtos.stream()
                                .filter(dto -> tierFilter == null || dto.getCustomerTier() == tierFilter)
                                .collect(Collectors.toList());

                // Manual pagination
                int start = page * size;
                int end = Math.min(start + size, filtered.size());
                List<CustomerDTO.CustomerListItemDto> pageContent = start < filtered.size()
                                ? filtered.subList(start, end)
                                : Collections.emptyList();

                int totalPages = (int) Math.ceil((double) filtered.size() / size);

                return CustomerDTO.CustomerListResponse.builder()
                                .customers(pageContent)
                                .totalElements((long) filtered.size())
                                .totalPages(totalPages)
                                .currentPage(page)
                                .pageSize(size)
                                .build();
        }

        /**
         * Get detailed customer information
         */
        @Transactional(readOnly = true)
        public CustomerDTO.CustomerDetailDto getCustomerDetail(UUID userId) {
                log.info("Getting customer detail for userId: {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Customer not found: " + userId));

                // Get all orders
                List<Order> orders = orderRepository.findByUserId(userId);

                // Calculate totalSpent from completed/delivered orders
                BigDecimal totalSpent = orders.stream()
                                .filter(o -> o.getStatus() != null &&
                                                (o.getStatus().name().equals("COMPLETED")
                                                                || o.getStatus().name().equals("DELIVERED")))
                                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate tier from totalSpent
                CustomerTier tier = CustomerTier.fromTotalSpent(totalSpent);

                // Get recent orders for display
                List<CustomerDTO.OrderSummaryDto> recentOrders = orders.stream()
                                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                                .limit(10)
                                .map(this::toOrderSummaryDto)
                                .collect(Collectors.toList());

                return CustomerDTO.CustomerDetailDto.builder()
                                .id(user.getId().toString())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .avatarUrl(user.getAvatarUrl())
                                .customerTier(tier)
                                .totalSpent(totalSpent)
                                .accountStatus(user.getAccountStatus())
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .orderCount((long) orders.size())
                                .recentOrders(recentOrders)
                                .build();
        }

        /**
         * Update customer account status
         */
        @Transactional
        public void updateCustomerStatus(UUID userId, AccountStatus status) {
                log.info("Updating customer status - userId: {}, status: {}", userId, status);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Customer not found: " + userId));

                user.setAccountStatus(status);
                userRepository.save(user);
        }

        /**
         * Get customer statistics
         */
        @Transactional(readOnly = true)
        public CustomerDTO.CustomerStatsDto getCustomerStats() {
                log.info("Getting customer statistics");

                List<User> allUsers = userRepository.findAll();

                long totalCustomers = allUsers.size();
                long activeCustomers = allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                                .count();
                long inactiveCustomers = allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.INACTIVE)
                                .count();
                long bannedCustomers = allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.BANNED)
                                .count();

                // Tier distribution
                Map<CustomerTier, Long> tierDistribution = allUsers.stream()
                                .collect(Collectors.groupingBy(
                                                user -> user.getCustomerTier() != null ? user.getCustomerTier()
                                                                : CustomerTier.BRONZE,
                                                Collectors.counting()));

                // Total revenue from all customers
                BigDecimal totalRevenue = allUsers.stream()
                                .map(u -> u.getTotalSpent() != null ? u.getTotalSpent() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return CustomerDTO.CustomerStatsDto.builder()
                                .totalCustomers(totalCustomers)
                                .activeCustomers(activeCustomers)
                                .inactiveCustomers(inactiveCustomers)
                                .bannedCustomers(bannedCustomers)
                                .tierDistribution(tierDistribution)
                                .totalRevenue(totalRevenue)
                                .build();
        }

        /**
         * Calculate and update total spent for a user
         * Called after order completion
         */
        @Transactional
        public void updateCustomerSpendingAndTier(UUID userId) {
                log.info("Updating customer spending and tier for userId: {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Customer not found: " + userId));

                // Calculate total from completed orders
                List<Order> completedOrders = orderRepository.findByUserId(userId).stream()
                                .filter(o -> o.getStatus() != null &&
                                                (o.getStatus().name().equals("COMPLETED")
                                                                || o.getStatus().name().equals("DELIVERED")))
                                .collect(Collectors.toList());

                BigDecimal totalSpent = completedOrders.stream()
                                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                user.setTotalSpent(totalSpent);
                user.setCustomerTier(CustomerTier.fromTotalSpent(totalSpent));

                userRepository.save(user);
                log.info("Updated customer tier for {} to {} with total spent: {}", userId, user.getCustomerTier(),
                                totalSpent);
        }

        /**
         * Sync/Recalculate spending for ALL customers
         * Useful for migration or data correction
         */
        @Transactional
        public void syncAllCustomerSpending() {
                log.info("Starting full customer spending sync...");
                List<User> allUsers = userRepository.findAll();
                int count = 0;
                for (User user : allUsers) {
                        try {
                                updateCustomerSpendingAndTier(user.getId());
                                count++;
                        } catch (Exception e) {
                                log.error("Failed to sync customer {}: {}", user.getId(), e.getMessage());
                        }
                }
                log.info("Completed full customer spending sync. Processed {} users.", count);
        }

        // Helper methods
        private boolean matchesSearch(User user, String search) {
                String searchLower = search.toLowerCase();
                return (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchLower)) ||
                                (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)) ||
                                (user.getPhone() != null && user.getPhone().toLowerCase().contains(searchLower));
        }

        private CustomerDTO.CustomerListItemDto toListItemDto(User user) {
                List<Order> orders = orderRepository.findByUserId(user.getId());
                long orderCount = orders.size();

                // Calculate totalSpent from completed/delivered orders
                BigDecimal totalSpent = orders.stream()
                                .filter(o -> o.getStatus() != null &&
                                                (o.getStatus().name().equals("COMPLETED")
                                                                || o.getStatus().name().equals("DELIVERED")))
                                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate tier from totalSpent
                CustomerTier tier = CustomerTier.fromTotalSpent(totalSpent);

                return CustomerDTO.CustomerListItemDto.builder()
                                .id(user.getId().toString())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .avatarUrl(user.getAvatarUrl())
                                .customerTier(tier)
                                .totalSpent(totalSpent)
                                .accountStatus(user.getAccountStatus())
                                .createdAt(user.getCreatedAt())
                                .orderCount(orderCount)
                                .build();
        }

        private CustomerDTO.OrderSummaryDto toOrderSummaryDto(Order order) {
                return CustomerDTO.OrderSummaryDto.builder()
                                .orderId(order.getId().toString())
                                .orderDate(order.getCreatedAt())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus() != null ? order.getStatus().name() : "UNKNOWN")
                                .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                                .build();
        }
}
