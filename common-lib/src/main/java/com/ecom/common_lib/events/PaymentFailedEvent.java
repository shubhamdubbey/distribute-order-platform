package com.ecom.common_lib.events;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    private UUID orderId;
    private UUID userId;
    private int amount;
    private String correlationId;
    private Instant failedAt;
}