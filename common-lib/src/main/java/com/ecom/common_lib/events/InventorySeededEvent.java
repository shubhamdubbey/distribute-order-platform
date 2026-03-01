package com.ecom.common_lib.events;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySeededEvent {
    private UUID productId;
    private int quantity;
    private String correlationId;
    private Instant seededAt;
}