package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.OrderDTO;
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
    public OrderDTO.Response getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional
    public OrderDTO.Response createOrder(OrderDTO.CreateRequest request) {
        User user = null;
        if(request.getUserId() != null) {
            // Try to find user by Firebase UID first
            user = userRepository.findByFirebaseUid(request.getUserId())
                    .orElse(null);
            
            // If not found, try as UUID
            if (user == null) {
                try {
                    UUID userId = UUID.fromString(request.getUserId());
                    user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid user ID format");
                }
            }
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
        response.setUserId(order.getUser().getId().toString());
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

    @Transactional
    public OrderDTO.Response addToCart(OrderDTO.AddToCartRequest request) {
        // Try to find user by Firebase UID first
        User user = userRepository.findByFirebaseUid(request.getUserId())
                .orElse(null);
        
        // If not found, try as UUID
        if (user == null) {
            try {
                UUID userId = UUID.fromString(request.getUserId());
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Find or create a PENDING order (cart) for the user
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        Order cart;

        if (pendingOrders.isEmpty()) {
            // Create new cart
            cart = new Order();
            cart.setUser(user);
            cart.setStatus(OrderStatus.PENDING);
            cart.setCreatedAt(LocalDateTime.now());
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setShippingFee(BigDecimal.ZERO);
            cart.setDiscountAmount(BigDecimal.ZERO);
            cart.setFinalAmount(BigDecimal.ZERO);
            cart.setOrderItems(new ArrayList<>());
        } else {
            // Use existing cart
            cart = pendingOrders.get(0);
        }

        // Check if product already exists in cart
        OrderItem existingItem = cart.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
        } else {
            // Add new item
            OrderItem newItem = OrderItem.builder()
                    .order(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cart.getOrderItems().add(newItem);
        }

        // Recalculate totals
        BigDecimal totalAmount = cart.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        cart.setFinalAmount(totalAmount);

        Order savedCart = orderRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional(readOnly = true)
    public OrderDTO.Response getCart(String userId) {
        // Try to find user by Firebase UID first
        User user = userRepository.findByFirebaseUid(userId)
                .orElse(null);
        
        // If not found, try as UUID
        UUID userUUID = null;
        if (user == null) {
            try {
                userUUID = UUID.fromString(userId);
                user = userRepository.findById(userUUID)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }
        
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            // Return empty cart
            Order emptyCart = new Order();
            emptyCart.setId(UUID.randomUUID());
            emptyCart.setUser(user);
            emptyCart.setStatus(OrderStatus.PENDING);
            emptyCart.setOrderItems(new ArrayList<>());
            emptyCart.setTotalAmount(BigDecimal.ZERO);
            emptyCart.setFinalAmount(BigDecimal.ZERO);
            emptyCart.setShippingFee(BigDecimal.ZERO);
            emptyCart.setDiscountAmount(BigDecimal.ZERO);
            return mapToResponse(emptyCart);
        }
        return mapToResponse(pendingOrders.get(0));
    }
}
