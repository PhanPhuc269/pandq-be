package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.OrderDTO;
import pandq.adapter.web.api.dtos.response.PaginationMetaDto;
import pandq.adapter.web.api.dtos.response.PaginationResponseDto;
import pandq.application.port.repositories.OrderRepository;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;
import pandq.domain.models.order.OrderItem;
import pandq.domain.models.product.Product;
import pandq.domain.models.user.User;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final JpaUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<OrderDTO.Response> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO.Response> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<OrderDTO.Response> searchUserOrders(UUID userId,
                                                                     String statusStr,
                                                                     String q,
                                                                     Integer page,
                                                                     Integer size) {
        OrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            // 'all' means no status filter
            if (!"all".equalsIgnoreCase(statusStr)) {
                try {
                    status = OrderStatus.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    // unknown status -> treat as no filter
                    status = null;
                }
            }
        }

        UUID orderId = null;
        if (q != null) {
            try {
                orderId = UUID.fromString(q.trim());
            } catch (Exception ignored) { }
        }

        PageRequest pageable = PageRequest.of(Optional.ofNullable(page).orElse(0),
                                              Optional.ofNullable(size).orElse(20),
                                              Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> result = orderRepository.searchUserOrders(userId, status, (q == null || q.isBlank()) ? null : q.trim(), orderId, pageable);
        List<OrderDTO.Response> data = result.getContent().stream().map(this::mapToResponse).toList();
        PaginationMetaDto meta = new PaginationMetaDto(result.getNumber(), result.getSize(), result.getTotalElements());
        return PaginationResponseDto.of(data, meta, "OK", "Search successful");
    }

    @Transactional(readOnly = true)
    public OrderDTO.Response getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional
    public OrderDTO.Response createOrder(OrderDTO.CreateRequest request) {
        User user = null;
        if(request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
             // TODO: get user from Security Context
             // For now throw error if no user ID
             throw new RuntimeException("User ID is required");
        }
        
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderDTO.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                    .build();
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(BigDecimal.ZERO); // Simplified
        order.setDiscountAmount(BigDecimal.ZERO); // Simplified
        order.setFinalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    private OrderDTO.Response mapToResponse(Order order) {
        OrderDTO.Response response = new OrderDTO.Response();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingFee(order.getShippingFee());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setFinalAmount(order.getFinalAmount());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderDTO.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderDTO.OrderItemResponse itemResponse = new OrderDTO.OrderItemResponse();
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setPrice(item.getPrice());
                    itemResponse.setTotalPrice(item.getTotalPrice());
                    return itemResponse;
                })
                .collect(Collectors.toList());
        response.setItems(items);

        return response;
    }
}
