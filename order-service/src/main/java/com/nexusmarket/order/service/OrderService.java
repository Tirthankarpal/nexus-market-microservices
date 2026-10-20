package com.nexusmarket.order.service;

import com.nexusmarket.order.dto.InventoryResponse;
import com.nexusmarket.order.dto.OrderItemDto;
import com.nexusmarket.order.dto.OrderRequest;
import com.nexusmarket.order.dto.ProductResponse;
import com.nexusmarket.order.event.OrderConfirmedEvent;
import com.nexusmarket.order.event.PaymentRequiredEvent;
import com.nexusmarket.order.event.PaymentCompletedEvent;
import com.nexusmarket.order.model.Order;
import com.nexusmarket.order.model.OrderItem;
import com.nexusmarket.order.model.OrderStatus;
import com.nexusmarket.order.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
@Transactional
@lombok.extern.slf4j.Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, 
                        WebClient.Builder webClientBuilder, 
                        KafkaTemplate<Object, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Places a new order, validating stock availability, verifying authoritative prices,
     * and deducting quantities atomically with rollback compensation on failure.
     */
    public Order placeOrder(OrderRequest orderRequest, String username) {
        List<OrderItemDto> orderLineItems = orderRequest.getOrderLineItemsList();
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        // 1. Collect SKU codes to query inventory status
        List<String> skuCodes = orderLineItems.stream()
                .map(OrderItemDto::getSkuCode)
                .toList();

        // 2. Query inventory-service for current stock levels with timeout
        InventoryResponse[] inventoryResponseArray = null;
        try {
            inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/v1/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .timeout(Duration.ofSeconds(4))
                    .block();
        } catch (Exception e) {
            log.error("Failed to contact inventory service: {}", e.getMessage());
            throw new IllegalStateException("Inventory service unavailable. Please try again shortly.");
        }

        if (inventoryResponseArray == null) {
            throw new IllegalArgumentException("Failed to verify inventory availability");
        }

        Map<String, InventoryResponse> inventoryMap = new HashMap<>();
        for (InventoryResponse res : inventoryResponseArray) {
            inventoryMap.put(res.skuCode(), res);
        }

        // 3. Verify stock sufficiency for every requested item
        for (OrderItemDto item : orderLineItems) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for SKU " + item.getSkuCode());
            }
            InventoryResponse stock = inventoryMap.get(item.getSkuCode());
            if (stock == null) {
                throw new IllegalArgumentException("Product SKU " + item.getSkuCode() + " does not exist in inventory.");
            }
            if (stock.quantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for SKU " + item.getSkuCode() + ". Available: " + stock.quantity() + ", Requested: " + item.getQuantity());
            }
        }

        // 4. Authoritative Price Verification: fetch official prices from product-service to prevent client-side tampering
        Map<String, BigDecimal> verifiedPrices = new HashMap<>();
        for (OrderItemDto item : orderLineItems) {
            BigDecimal price = null;
            try {
                ProductResponse prod = webClientBuilder.build().get()
                        .uri("http://product-service/api/v1/products/by-name/{name}", item.getSkuCode())
                        .retrieve()
                        .bodyToMono(ProductResponse.class)
                        .timeout(Duration.ofSeconds(3))
                        .block();
                if (prod != null && prod.price() != null) {
                    price = prod.price();
                }
            } catch (Exception e) {
                log.warn("Could not fetch price from product-service for SKU {}. Falling back to provided price.", item.getSkuCode());
            }

            if (price == null) {
                if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invalid price for item: " + item.getSkuCode());
                }
                price = item.getPrice();
            }
            verifiedPrices.put(item.getSkuCode(), price);
        }

        // 5. Deduct inventory levels synchronously with compensation rollback on partial failure
        List<OrderItemDto> successfullyDeducted = new ArrayList<>();
        try {
            for (OrderItemDto item : orderLineItems) {
                webClientBuilder.build().put()
                        .uri("http://inventory-service/api/v1/inventory/deduct",
                                uriBuilder -> uriBuilder
                                        .queryParam("skuCode", item.getSkuCode())
                                        .queryParam("quantity", item.getQuantity())
                                        .build())
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(4))
                        .block();
                successfullyDeducted.add(item);
            }
        } catch (Exception e) {
            log.error("Error deducting stock during checkout, triggering rollback compensation: {}", e.getMessage());
            rollbackDeductions(successfullyDeducted);
            throw new IllegalStateException("Failed to reserve inventory: " + e.getMessage());
        }

        // 6. Build Order Entity with verified prices
        List<OrderItem> itemsList = orderLineItems.stream()
                .map(dto -> OrderItem.builder()
                        .skuCode(dto.getSkuCode())
                        .price(verifiedPrices.get(dto.getSkuCode()))
                        .quantity(dto.getQuantity())
                        .build())
                .toList();

        BigDecimal totalAmount = itemsList.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .userEmail(username)
                .orderLineItemsList(itemsList)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 7. Dispatch payment-required event via Kafka
        if (kafkaTemplate != null) {
            try {
                PaymentRequiredEvent event = new PaymentRequiredEvent(savedOrder.getOrderNumber(), username, totalAmount);
                kafkaTemplate.send("payment-required", event).get(); // Synchronous block to guarantee delivery
            } catch (Exception e) {
                log.error("Failed to publish payment-required event to Kafka: {}. Rolling back order and inventory.", e.getMessage(), e);
                rollbackDeductions(successfullyDeducted);
                savedOrder.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(savedOrder);
                throw new IllegalStateException("Payment service is currently unavailable. Order could not be placed.");
            }
        }

        return savedOrder;
    }

    /**
     * Handles payment results idempotently.
     */
    @KafkaListener(topics = "payment-completed", groupId = "order-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Order order = orderRepository.findByOrderNumber(event.orderNumber());
        if (order == null) {
            log.error("Order not found for orderNumber: {}", event.orderNumber());
            return;
        }

        // Idempotency check: only process events for orders currently in PENDING state
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is already in status {}, skipping duplicate payment-completed event", 
                    event.orderNumber(), order.getStatus());
            return;
        }

        if (event.success()) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            if (kafkaTemplate != null) {
                OrderConfirmedEvent confirmedEvent = new OrderConfirmedEvent(order.getOrderNumber(), order.getUserEmail());
                kafkaTemplate.send("order-confirmed", confirmedEvent);
            }
            log.info("Order {} confirmed successfully.", event.orderNumber());
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.warn("Payment failed for order {}. Restoring inventory.", event.orderNumber());

            // Rollback inventory for the failed order
            for (OrderItem item : order.getOrderLineItemsList()) {
                restoreItemStock(item.getSkuCode(), item.getQuantity());
            }
        }
    }

    private void rollbackDeductions(List<OrderItemDto> itemsToRestore) {
        for (OrderItemDto item : itemsToRestore) {
            restoreItemStock(item.getSkuCode(), item.getQuantity());
        }
    }

    private void restoreItemStock(String skuCode, Integer quantity) {
        try {
            webClientBuilder.build().put()
                    .uri("http://inventory-service/api/v1/inventory/restore",
                            uriBuilder -> uriBuilder
                                    .queryParam("skuCode", skuCode)
                                    .queryParam("quantity", quantity)
                                    .build())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(4))
                    .block();
        } catch (Exception e) {
            log.error("Failed to restore inventory for SKU: {} quantity: {}", skuCode, quantity, e);
        }
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getOrdersByUser(String username, org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findByUserEmail(username, pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getAllOrders(org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}
