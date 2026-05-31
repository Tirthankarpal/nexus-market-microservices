package com.nexusmarket.order.controller;

import com.nexusmarket.order.dto.OrderRequest;
import com.nexusmarket.order.model.Order;
import com.nexusmarket.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Places a new order. Associates the order with the logged-in user.
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestBody OrderRequest orderRequest,
            @RequestHeader("X-Authenticated-User") String username) {
        Order placedOrder = orderService.placeOrder(orderRequest, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(placedOrder);
    }

    /**
     * Retrieves order history. Standard USER sees only their own history,
     * while ADMIN sees all orders in the system.
     */
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestHeader("X-Authenticated-User") String username,
            @RequestHeader("X-Authenticated-Role") String role) {
        
        // ADMIN gets to see all orders in the entire system.
        if (role != null && role.equalsIgnoreCase("ADMIN")) {
            return ResponseEntity.ok(orderService.getAllOrders());
        }
        
        // Standard USER only gets to see their own history.
        return ResponseEntity.ok(orderService.getOrdersByUser(username));
    }
}
