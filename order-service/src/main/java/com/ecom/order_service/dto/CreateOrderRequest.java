package com.ecom.order_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderRequest {

    // Note the `requestId` field. This is not a DB field — it's the idempotency key. The client sends it, we check it against Redis before doing anything.
    private String requestId;
    private UUID userId;
    private UUID productId;
    private int quantity;
}