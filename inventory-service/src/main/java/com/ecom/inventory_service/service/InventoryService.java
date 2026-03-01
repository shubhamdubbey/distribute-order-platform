package com.ecom.inventory_service.service;


import com.ecom.common_lib.events.InventorySeededEvent;
import com.ecom.common_lib.events.OrderCreatedEvent;
import com.ecom.inventory_service.dto.InventoryResponse;
import com.ecom.inventory_service.entity.Inventory;
import com.ecom.inventory_service.event.InventoryEventPublisher;
import com.ecom.inventory_service.event.InventoryFailedEvent;
import com.ecom.inventory_service.event.InventoryReservedEvent;
import com.ecom.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    public void processOrderCreated(OrderCreatedEvent event, String correlationId) {

        log.info("Processing inventory for orderId: {}", event.getOrderId());

        // Find inventory with DB-level lock
        Inventory inventory = inventoryRepository
                .findByProductIdWithLock(event.getProductId())
                .orElse(null);

        if (inventory == null) {
            log.error("Product not found in inventory. productId: {}", event.getProductId());
            publishFailedEvent(event, correlationId, "Product not found");
            return;
        }

        if (inventory.getAvailableQuantity() < event.getQuantity()) {
            log.error("Insufficient stock in DB. Available: {}, Requested: {}",
                    inventory.getAvailableQuantity(), event.getQuantity());
            publishFailedEvent(event, correlationId, "Insufficient stock");
            return;
        }

        // Reserve inventory
        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - event.getQuantity());
        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + event.getQuantity());

        inventoryRepository.save(inventory);

        log.info("Inventory reserved for orderId: {}. Remaining: {}",
                event.getOrderId(), inventory.getAvailableQuantity());

        // Publish success event
        InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .correlationId(correlationId)
                .reservedAt(Instant.now())
                .build();

        eventPublisher.publishInventoryReserved(reservedEvent);
    }

    @Transactional
    public InventoryResponse seedInventory(String productId, int quantity) {

        // Check if inventory already exists for this product
        Inventory inventory = inventoryRepository
                .findByProductId(UUID.fromString(productId))
                .orElse(null);

        if (inventory != null) {
            // Update existing
            inventory.setAvailableQuantity(quantity);
            inventory.setReservedQuantity(0);
            inventoryRepository.save(inventory);
            log.info("Inventory updated for productId: {}", productId);
        } else {
            // Create new
            inventory = Inventory.builder()
                    .productId(UUID.fromString(productId))
                    .availableQuantity(quantity)
                    .reservedQuantity(0)
                    .build();
            inventoryRepository.save(inventory);
            log.info("Inventory created for productId: {}", productId);
        }

        // Publish event so order-service can sync Redis
        InventorySeededEvent event = InventorySeededEvent.builder()
                .productId(UUID.fromString(productId))
                .quantity(quantity)
                .correlationId(UUID.randomUUID().toString())
                .seededAt(Instant.now())
                .build();

        eventPublisher.publishInventorySeeded(event);

        return mapToResponse(inventory);
    }

    public InventoryResponse getInventory(String productId) {
        Inventory inventory = inventoryRepository
                .findByProductId(UUID.fromString(productId))
                .orElseThrow(() -> new RuntimeException(
                        "Inventory not found for productId: " + productId));

        return mapToResponse(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .build();
    }

    private void publishFailedEvent(OrderCreatedEvent event,
                                    String correlationId, String reason) {
        InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .reason(reason)
                .correlationId(correlationId)
                .failedAt(Instant.now())
                .build();

        eventPublisher.publishInventoryFailed(failedEvent);
    }
}