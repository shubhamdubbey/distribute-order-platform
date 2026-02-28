package com.ecom.common_lib.events;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private UUID orderId;
    private String status;
    private Instant processedAt;
}