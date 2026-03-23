package com.ecom.inventory_service.controller;

import com.ecom.inventory_service.dto.InventoryResponse;
import com.ecom.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/seed")
    public ResponseEntity<InventoryResponse> seedInventory(
            @RequestParam String productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.seedInventory(productId, quantity));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }
}