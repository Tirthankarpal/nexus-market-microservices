package com.nexusmarket.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusmarket.inventory.dto.InventoryResponse;
import com.nexusmarket.inventory.exception.InsufficientStockException;
import com.nexusmarket.inventory.exception.InventoryNotFoundException;
import com.nexusmarket.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("GET /api/v1/inventory: Should return list of stock status")
    void isInStock_ShouldReturnStockStatus() throws Exception {
        // Arrange
        List<String> skuCodes = List.of("SKU1", "SKU2");
        List<InventoryResponse> responseList = List.of(
                new InventoryResponse("SKU1", true, 10),
                new InventoryResponse("SKU2", false, 0)
        );

        when(inventoryService.isInStock(skuCodes)).thenReturn(responseList);

        // Act & Assert
        mockMvc.perform(get("/api/v1/inventory")
                        .param("skuCode", "SKU1")
                        .param("skuCode", "SKU2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skuCode").value("SKU1"))
                .andExpect(jsonPath("$[0].isInStock").value(true))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[1].skuCode").value("SKU2"))
                .andExpect(jsonPath("$[1].isInStock").value(false))
                .andExpect(jsonPath("$[1].quantity").value(0));

        verify(inventoryService, times(1)).isInStock(skuCodes);
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/deduct: Should return HTTP 200 on success")
    void deductStock_ShouldSucceed() throws Exception {
        // Arrange
        doNothing().when(inventoryService).deductStock("SKU1", 5);

        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/deduct")
                        .param("skuCode", "SKU1")
                        .param("quantity", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).deductStock("SKU1", 5);
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/deduct: Should return HTTP 404 when product not found")
    void deductStock_ShouldReturn404_WhenProductNotFound() throws Exception {
        // Arrange
        doThrow(new InventoryNotFoundException("Product not found in inventory: SKU1"))
                .when(inventoryService).deductStock("SKU1", 5);

        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/deduct")
                        .param("skuCode", "SKU1")
                        .param("quantity", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found in inventory: SKU1"));

        verify(inventoryService, times(1)).deductStock("SKU1", 5);
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/deduct: Should return HTTP 400 when stock is insufficient")
    void deductStock_ShouldReturn400_WhenStockIsInsufficient() throws Exception {
        // Arrange
        doThrow(new InsufficientStockException("Insufficient stock for SKU SKU1"))
                .when(inventoryService).deductStock("SKU1", 10);

        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/deduct")
                        .param("skuCode", "SKU1")
                        .param("quantity", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for SKU SKU1"));

        verify(inventoryService, times(1)).deductStock("SKU1", 10);
    }

    @Test
    @DisplayName("POST /api/v1/inventory: Should return HTTP 201 when role is ADMIN")
    void addStock_ShouldSucceed_WhenRoleIsAdmin() throws Exception {
        // Arrange
        doNothing().when(inventoryService).addStock("SKU1", 100);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory")
                        .param("skuCode", "SKU1")
                        .param("quantity", "100")
                        .header("X-Authenticated-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string("Stock updated successfully"));

        verify(inventoryService, times(1)).addStock("SKU1", 100);
    }

    @Test
    @DisplayName("POST /api/v1/inventory: Should return HTTP 403 when role is USER")
    void addStock_ShouldReturn403_WhenRoleIsUser() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory")
                        .param("skuCode", "SKU1")
                        .param("quantity", "100")
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only ADMIN can stock inventory items"));

        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("POST /api/v1/inventory: Should return HTTP 403 when role is missing")
    void addStock_ShouldReturn403_WhenRoleIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory")
                        .param("skuCode", "SKU1")
                        .param("quantity", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only ADMIN can stock inventory items"));

        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/set: Should return HTTP 200 when role is ADMIN")
    void setStock_ShouldSucceed_WhenRoleIsAdmin() throws Exception {
        // Arrange
        doNothing().when(inventoryService).setStock("SKU1", 50);

        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/set")
                        .param("skuCode", "SKU1")
                        .param("quantity", "50")
                        .header("X-Authenticated-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Stock level updated successfully"));

        verify(inventoryService, times(1)).setStock("SKU1", 50);
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/set: Should return HTTP 403 when role is USER")
    void setStock_ShouldReturn403_WhenRoleIsUser() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/set")
                        .param("skuCode", "SKU1")
                        .param("quantity", "50")
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only ADMIN can set stock levels"));

        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/set: Should return HTTP 403 when role is missing")
    void setStock_ShouldReturn403_WhenRoleIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/inventory/set")
                        .param("skuCode", "SKU1")
                        .param("quantity", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only ADMIN can set stock levels"));

        verifyNoInteractions(inventoryService);
    }
}
