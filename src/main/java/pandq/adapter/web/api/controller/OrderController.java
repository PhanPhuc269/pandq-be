package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.OrderDTO;
import pandq.application.services.OrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDTO.Response>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO.Response>> getOrdersByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO.Response> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    public ResponseEntity<OrderDTO.Response> createOrder(@RequestBody OrderDTO.CreateRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/cart/add")
    public ResponseEntity<OrderDTO.Response> addToCart(@RequestBody OrderDTO.AddToCartRequest request) {
        return ResponseEntity.ok(orderService.addToCart(request));
    }

    @PostMapping("/cart/decrease")
    public ResponseEntity<OrderDTO.Response> decreaseQuantity(@RequestBody OrderDTO.AddToCartRequest request) {
        return ResponseEntity.ok(orderService.decreaseQuantity(request));
    }

    @DeleteMapping("/cart/{userId}/{productId}")
    public ResponseEntity<OrderDTO.Response> removeFromCart(
            @PathVariable String userId,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(orderService.removeFromCart(userId, productId));
    }

    @GetMapping("/cart/{userId}")
    public ResponseEntity<OrderDTO.Response> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getCart(userId));
    }

    @PostMapping("/cart/merge/{userId}")
    public ResponseEntity<OrderDTO.Response> mergeGuestCart(
            @PathVariable String userId,
            @RequestBody List<OrderDTO.AddToCartRequest> guestCartItems) {
        return ResponseEntity.ok(orderService.mergeGuestCart(userId, guestCartItems));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO.Response> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody OrderDTO.UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }
}
