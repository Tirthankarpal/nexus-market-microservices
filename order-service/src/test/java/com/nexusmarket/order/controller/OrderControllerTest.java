package com.nexusmarket.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusmarket.order.dto.OrderItemDto;
import com.nexusmarket.order.dto.OrderRequest;
import com.nexusmarket.order.model.Order;
import com.nexusmarket.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/v1/orders: Should place order and return HTTP 201 Created")
    void placeOrder_ShouldReturnCreated() throws Exception {
        // Arrange
        String username = "customer@nexus.com";
        OrderItemDto itemDto = new OrderItemDto("SKU123", new BigDecimal("49.99"), 1);
        OrderRequest request = new OrderRequest(List.of(itemDto));

        Order expectedOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-12345")
                .userEmail(username)
                .totalAmount(new BigDecimal("49.99"))
                .build();

        when(orderService.placeOrder(any(OrderRequest.class), eq(username))).thenReturn(expectedOrder);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Authenticated-User", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345"))
                .andExpect(jsonPath("$.userEmail").value(username))
                .andExpect(jsonPath("$.totalAmount").value(49.99));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class), eq(username));
    }

    @Test
    @DisplayName("GET /api/v1/orders: Should return all orders when user has ADMIN role")
    void getOrders_ShouldReturnAllOrders_WhenRoleIsAdmin() throws Exception {
        // Arrange
        String username = "admin@nexus.com";
        Order order = Order.builder().id(1L).orderNumber("ORD-ADMIN").userEmail("customer@nexus.com").build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Authenticated-User", username)
                        .header("X-Authenticated-Role", "ADMIN")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService, times(1)).getAllOrders(any(Pageable.class));
        verify(orderService, never()).getOrdersByUser(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/orders: Should return only user's orders when user has USER role")
    void getOrders_ShouldReturnUserOrders_WhenRoleIsUser() throws Exception {
        // Arrange
        String username = "customer@nexus.com";
        Order order = Order.builder().id(1L).orderNumber("ORD-USER").userEmail(username).build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderService.getOrdersByUser(eq(username), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Authenticated-User", username)
                        .header("X-Authenticated-Role", "USER")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-USER"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService, times(1)).getOrdersByUser(eq(username), any(Pageable.class));
        verify(orderService, never()).getAllOrders(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/orders: Should default to user's orders when role header is missing")
    void getOrders_ShouldReturnUserOrders_WhenRoleIsMissing() throws Exception {
        // Arrange
        String username = "customer@nexus.com";
        Order order = Order.builder().id(1L).orderNumber("ORD-NO-ROLE").userEmail(username).build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderService.getOrdersByUser(eq(username), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Authenticated-User", username)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-NO-ROLE"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService, times(1)).getOrdersByUser(eq(username), any(Pageable.class));
        verify(orderService, never()).getAllOrders(any(Pageable.class));
    }
}
