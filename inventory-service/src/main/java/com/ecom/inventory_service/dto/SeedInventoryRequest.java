package com.ecom.inventory_service.dto;

import lombok.Data;

@Data
public class SeedInventoryRequest {
    private String productId;
    private int quantity;
}