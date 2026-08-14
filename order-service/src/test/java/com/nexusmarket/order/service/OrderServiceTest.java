package com.nexusmarket.order.service;

import com.nexusmarket.order.dto.InventoryResponse;
import com.nexusmarket.order.dto.OrderItemDto;
import com.nexusmarket.order.dto.OrderRequest;
import com.nexusmarket.order.event.PaymentRequiredEvent;
import com.nexusmarket.order.model.Order;
import com.nexusmarket.order.model.OrderItem;
import com.nexusmarket.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    private OrderService orderService;
    private OrderService orderServiceNoKafka;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, webClientBuilder, kafkaTemplate);
        orderServiceNoKafka = new OrderService(orderRepository, webClientBuilder, null);
    }

    private void mockWebClientGet(InventoryResponse[] responses) {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InventoryResponse[].class)).thenReturn(Mono.just(responses));
    }

    private void mockWebClientPut() {
        when(webClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));
    }

    @Test
    @DisplayName("placeOrder: Should successfully place order and publish Kafka event when stock is available")
    void placeOrder_ShouldSucceed_WhenStockIsAvailable() {
        // Arrange
        String username = "user@nexus.com";
        OrderItemDto itemDto1 = new OrderItemDto("SKU1", new BigDecimal("10.00"), 2);
        OrderItemDto itemDto2 = new OrderItemDto("SKU2", new BigDecimal("15.00"), 1);
        
        OrderRequest request = new OrderRequest(List.of(itemDto1, itemDto2));

        InventoryResponse[] mockInventory = {
                new InventoryResponse("SKU1", true, 10),
                new InventoryResponse("SKU2", true, 5)
        };

        mockWebClientGet(mockInventory);
        mockWebClientPut();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        // Act
        Order placedOrder = orderService.placeOrder(request, username);

        // Assert
        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getId()).isEqualTo(99L);
        assertThat(placedOrder.getUserEmail()).isEqualTo(username);
        assertThat(placedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("35.00")); // (10*2) + (15*1)
        assertThat(placedOrder.getOrderLineItemsList()).hasSize(2);

        // Verify Repository called
        verify(orderRepository, times(1)).save(any(Order.class));

        // Verify WebClient calls
        verify(webClientBuilder, times(3)).build(); // 1 build for GET, 2 builds for 2 PUTs
        verify(webClient, times(1)).get();
        verify(webClient, times(2)).put();

        // Verify Kafka Event
        ArgumentCaptor<PaymentRequiredEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRequiredEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("payment-required"), eventCaptor.capture());
        PaymentRequiredEvent event = eventCaptor.getValue();
        assertThat(event.userEmail()).isEqualTo(username);
        assertThat(event.orderNumber()).isEqualTo(placedOrder.getOrderNumber());
        assertThat(event.amount()).isEqualByComparingTo(placedOrder.getTotalAmount());
    }

    @Test
    @DisplayName("placeOrder: Should throw IllegalArgumentException when cart is empty")
    void placeOrder_ShouldThrowException_WhenCartIsEmpty() {
        // Arrange
        OrderRequest request = new OrderRequest(new ArrayList<>());

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(request, "user@nexus.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart cannot be empty");

        verifyNoInteractions(orderRepository, webClientBuilder, kafkaTemplate);
    }

    @Test
    @DisplayName("placeOrder: Should throw IllegalArgumentException when cart is null")
    void placeOrder_ShouldThrowException_WhenCartIsNull() {
        // Arrange
        OrderRequest request = new OrderRequest(null);

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(request, "user@nexus.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart cannot be empty");

        verifyNoInteractions(orderRepository, webClientBuilder, kafkaTemplate);
    }

    @Test
    @DisplayName("placeOrder: Should throw IllegalArgumentException when inventory response is null")
    void placeOrder_ShouldThrowException_WhenInventoryResponseIsNull() {
        // Arrange
        OrderItemDto itemDto = new OrderItemDto("SKU1", new BigDecimal("10.00"), 2);
        OrderRequest request = new OrderRequest(List.of(itemDto));

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InventoryResponse[].class)).thenReturn(Mono.empty()); // Returns null in block()

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(request, "user@nexus.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to contact inventory service");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("placeOrder: Should throw IllegalArgumentException when SKU does not exist in inventory service response")
    void placeOrder_ShouldThrowException_WhenSkuNotFoundInInventory() {
        // Arrange
        OrderItemDto itemDto = new OrderItemDto("SKU_UNKNOWN", new BigDecimal("10.00"), 2);
        OrderRequest request = new OrderRequest(List.of(itemDto));

        InventoryResponse[] mockInventory = {
                new InventoryResponse("SKU_OTHER", true, 10)
        };

        mockWebClientGet(mockInventory);

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(request, "user@nexus.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product SKU SKU_UNKNOWN does not exist in inventory.");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("placeOrder: Should throw IllegalArgumentException when stock is insufficient")
    void placeOrder_ShouldThrowException_WhenStockIsInsufficient() {
        // Arrange
        OrderItemDto itemDto = new OrderItemDto("SKU1", new BigDecimal("10.00"), 5);
        OrderRequest request = new OrderRequest(List.of(itemDto));

        InventoryResponse[] mockInventory = {
                new InventoryResponse("SKU1", true, 3) // Only 3 in stock, requested 5
        };

        mockWebClientGet(mockInventory);

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(request, "user@nexus.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient stock for SKU SKU1. Available: 3, Requested: 5");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("placeOrder: Should succeed without rolling back order when Kafka dispatch fails")
    void placeOrder_ShouldSucceed_WhenKafkaThrowsException() {
        // Arrange
        String username = "user@nexus.com";
        OrderItemDto itemDto = new OrderItemDto("SKU1", new BigDecimal("10.00"), 1);
        OrderRequest request = new OrderRequest(List.of(itemDto));

        InventoryResponse[] mockInventory = {
                new InventoryResponse("SKU1", true, 10)
        };

        mockWebClientGet(mockInventory);
        mockWebClientPut();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(101L);
            return order;
        });

        // Simulate Kafka failure
        doThrow(new RuntimeException("Kafka Broker Connection Error"))
                .when(kafkaTemplate).send(anyString(), any(PaymentRequiredEvent.class));

        // Act
        Order placedOrder = orderService.placeOrder(request, username);

        // Assert
        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getId()).isEqualTo(101L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaTemplate, times(1)).send(eq("payment-required"), any(PaymentRequiredEvent.class));
    }

    @Test
    @DisplayName("placeOrder: Should succeed without trying to publish event when Kafka template is not configured")
    void placeOrder_ShouldSucceed_WhenKafkaTemplateIsMissing() {
        // Arrange
        String username = "user@nexus.com";
        OrderItemDto itemDto = new OrderItemDto("SKU1", new BigDecimal("10.00"), 1);
        OrderRequest request = new OrderRequest(List.of(itemDto));

        InventoryResponse[] mockInventory = {
                new InventoryResponse("SKU1", true, 10)
        };

        mockWebClientGet(mockInventory);
        mockWebClientPut();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(102L);
            return order;
        });

        // Act
        Order placedOrder = orderServiceNoKafka.placeOrder(request, username);

        // Assert
        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getId()).isEqualTo(102L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("getOrdersByUser: Should return user orders page from repository")
    void getOrdersByUser_ShouldReturnUserOrders() {
        // Arrange
        String username = "user@nexus.com";
        Pageable pageable = PageRequest.of(0, 10);
        Order order = Order.builder().id(1L).userEmail(username).build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findByUserEmail(username, pageable)).thenReturn(page);

        // Act
        Page<Order> result = orderService.getOrdersByUser(username, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserEmail()).isEqualTo(username);
        verify(orderRepository, times(1)).findByUserEmail(username, pageable);
    }

    @Test
    @DisplayName("getAllOrders: Should return all orders page from repository")
    void getAllOrders_ShouldReturnAllOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Order order1 = Order.builder().id(1L).userEmail("user1@nexus.com").build();
        Order order2 = Order.builder().id(2L).userEmail("user2@nexus.com").build();
        Page<Order> page = new PageImpl<>(List.of(order1, order2));

        when(orderRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<Order> result = orderService.getAllOrders(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(orderRepository, times(1)).findAll(pageable);
    }
}
