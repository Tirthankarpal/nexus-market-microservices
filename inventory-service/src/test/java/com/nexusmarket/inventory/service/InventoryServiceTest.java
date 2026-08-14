package com.nexusmarket.inventory.service;

import com.nexusmarket.inventory.dto.InventoryResponse;
import com.nexusmarket.inventory.exception.InsufficientStockException;
import com.nexusmarket.inventory.exception.InventoryNotFoundException;
import com.nexusmarket.inventory.model.Inventory;
import com.nexusmarket.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("isInStock: Should return correct stock status and quantity for existing SKUs")
    void isInStock_ShouldReturnCorrectStatus_WhenSkuExists() {
        // Arrange
        String sku1 = "SKU123";
        String sku2 = "SKU456";
        Inventory inv1 = Inventory.builder().id(1L).skuCode(sku1).quantity(10).build();
        Inventory inv2 = Inventory.builder().id(2L).skuCode(sku2).quantity(0).build();

        when(inventoryRepository.findBySkuCodeIn(List.of(sku1, sku2))).thenReturn(List.of(inv1, inv2));

        // Act
        List<InventoryResponse> result = inventoryService.isInStock(List.of(sku1, sku2));

        // Assert
        assertThat(result).hasSize(2);
        
        InventoryResponse resp1 = result.stream().filter(r -> r.skuCode().equals(sku1)).findFirst().orElseThrow();
        assertThat(resp1.isInStock()).isTrue();
        assertThat(resp1.quantity()).isEqualTo(10);

        InventoryResponse resp2 = result.stream().filter(r -> r.skuCode().equals(sku2)).findFirst().orElseThrow();
        assertThat(resp2.isInStock()).isFalse();
        assertThat(resp2.quantity()).isZero();

        verify(inventoryRepository, times(1)).findBySkuCodeIn(List.of(sku1, sku2));
    }

    @Test
    @DisplayName("isInStock: Should return empty list when no matching SKUs in DB")
    void isInStock_ShouldReturnEmptyList_WhenNoSkuMatches() {
        // Arrange
        when(inventoryRepository.findBySkuCodeIn(any())).thenReturn(Collections.emptyList());

        // Act
        List<InventoryResponse> result = inventoryService.isInStock(List.of("NON-EXISTENT"));

        // Assert
        assertThat(result).isEmpty();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(List.of("NON-EXISTENT"));
    }

    @Test
    @DisplayName("deductStock: Should deduct stock successfully when inventory is sufficient")
    void deductStock_ShouldSucceed_WhenQuantityIsSufficient() {
        // Arrange
        String sku = "IPHONE15";
        Inventory inventory = Inventory.builder().id(1L).skuCode(sku).quantity(10).build();

        when(inventoryRepository.findBySkuCodeWithLock(sku)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryService.deductStock(sku, 3);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).findBySkuCodeWithLock(sku);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getQuantity()).isEqualTo(7);
        assertThat(savedInventory.getSkuCode()).isEqualTo(sku);
    }

    @Test
    @DisplayName("deductStock: Should throw InventoryNotFoundException when SKU does not exist")
    void deductStock_ShouldThrowNotFound_WhenSkuDoesNotExist() {
        // Arrange
        String sku = "UNKNOWN";
        when(inventoryRepository.findBySkuCodeWithLock(sku)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.deductStock(sku, 5))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("Product not found in inventory: UNKNOWN");

        verify(inventoryRepository, times(1)).findBySkuCodeWithLock(sku);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("deductStock: Should throw InsufficientStockException when quantity requested exceeds available")
    void deductStock_ShouldThrowInsufficientStock_WhenQuantityIsInsufficient() {
        // Arrange
        String sku = "IPHONE15";
        Inventory inventory = Inventory.builder().id(1L).skuCode(sku).quantity(4).build();

        when(inventoryRepository.findBySkuCodeWithLock(sku)).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.deductStock(sku, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for SKU IPHONE15. Available: 4, Requested: 5");

        verify(inventoryRepository, times(1)).findBySkuCodeWithLock(sku);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("addStock: Should increment quantity of existing inventory item")
    void addStock_ShouldIncrementQuantity_WhenSkuExists() {
        // Arrange
        String sku = "SAMSUNG-S24";
        Inventory inventory = Inventory.builder().id(1L).skuCode(sku).quantity(5).build();

        when(inventoryRepository.findBySkuCode(sku)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryService.addStock(sku, 15);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).findBySkuCode(sku);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("addStock: Should create new inventory item with target quantity when SKU does not exist")
    void addStock_ShouldCreateNewItem_WhenSkuDoesNotExist() {
        // Arrange
        String sku = "NEW-ITEM";
        when(inventoryRepository.findBySkuCode(sku)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryService.addStock(sku, 8);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).findBySkuCode(sku);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getSkuCode()).isEqualTo(sku);
        assertThat(saved.getQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("setStock: Should set absolute quantity of existing inventory item")
    void setStock_ShouldSetAbsoluteQuantity_WhenSkuExists() {
        // Arrange
        String sku = "SAMSUNG-S24";
        Inventory inventory = Inventory.builder().id(1L).skuCode(sku).quantity(5).build();

        when(inventoryRepository.findBySkuCode(sku)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryService.setStock(sku, 50);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).findBySkuCode(sku);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("setStock: Should create new inventory item with target quantity when SKU does not exist")
    void setStock_ShouldCreateNewItem_WhenSkuDoesNotExist() {
        // Arrange
        String sku = "NEW-ITEM";
        when(inventoryRepository.findBySkuCode(sku)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryService.setStock(sku, 12);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).findBySkuCode(sku);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getSkuCode()).isEqualTo(sku);
        assertThat(saved.getQuantity()).isEqualTo(12);
    }
}
