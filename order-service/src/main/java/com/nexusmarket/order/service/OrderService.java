package com.nexusmarket.order.service;

import com.nexusmarket.order.dto.InventoryResponse;
import com.nexusmarket.order.dto.OrderItemDto;
import com.nexusmarket.order.dto.OrderRequest;
import com.nexusmarket.order.event.OrderPlacedEvent;
import com.nexusmarket.order.model.Order;
import com.nexusmarket.order.model.OrderItem;
import com.nexusmarket.order.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, 
                        WebClient.Builder webClientBuilder, 
                        java.util.Optional<KafkaTemplate<String, OrderPlacedEvent>> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
        this.kafkaTemplate = kafkaTemplate.orElse(null);
    }

    /**
     * Places a new order, validating stock availability and deducting quantities atomically.
     */
    public Order placeOrder(OrderRequest orderRequest, String username) {
        List<OrderItemDto> orderLineItems = orderRequest.getOrderLineItemsList();
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        // Collect SKU codes to query inventory status
        List<String> skuCodes = orderLineItems.stream()
                .map(OrderItemDto::getSkuCode)
                .toList();

        // Query inventory-service for current stock levels
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/v1/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        if (inventoryResponseArray == null) {
            throw new IllegalArgumentException("Failed to contact inventory service");
        }

        Map<String, InventoryResponse> inventoryMap = new HashMap<>();
        for (InventoryResponse res : inventoryResponseArray) {
            inventoryMap.put(res.skuCode(), res);
        }

        // Verify that stock is sufficient for each requested item
        for (OrderItemDto item : orderLineItems) {
            InventoryResponse stock = inventoryMap.get(item.getSkuCode());
            if (stock == null) {
                throw new IllegalArgumentException("Product SKU " + item.getSkuCode() + " does not exist in inventory.");
            }
            if (stock.quantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for SKU " + item.getSkuCode() + ". Available: " + stock.quantity() + ", Requested: " + item.getQuantity());
            }
        }

        // Deduct inventory levels synchronously in inventory-service
        for (OrderItemDto item : orderLineItems) {
            webClientBuilder.build().put()
                    .uri("http://inventory-service/api/v1/inventory/deduct",
                            uriBuilder -> uriBuilder
                                    .queryParam("skuCode", item.getSkuCode())
                                    .queryParam("quantity", item.getQuantity())
                                    .build())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        }

        // Map items and save order entity
        List<OrderItem> itemsList = orderLineItems.stream()
                .map(this::mapToEntity)
                .toList();

        BigDecimal totalAmount = itemsList.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .userEmail(username)
                .orderLineItemsList(itemsList)
                .totalAmount(totalAmount)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Dispatch order confirmation event via Kafka asynchronously
        if (kafkaTemplate != null) {
            try {
                OrderPlacedEvent event = new OrderPlacedEvent(savedOrder.getOrderNumber(), username);
                kafkaTemplate.send("order-placed", event);
            } catch (Exception e) {
                // Log dispatch failures to prevent rolling back successful order creations
                System.err.println("Failed to publish order event to Kafka: " + e.getMessage());
            }
        }

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(String userEmail) {
        return orderRepository.findByUserEmail(userEmail);
    }

    private OrderItem mapToEntity(OrderItemDto dto) {
        return OrderItem.builder()
                .skuCode(dto.getSkuCode())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .build();
    }
}
