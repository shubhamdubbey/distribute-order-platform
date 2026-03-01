package com.ecom.inventory_service.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private UUID productId;
    private int availableQuantity;
    private int reservedQuantity;
}