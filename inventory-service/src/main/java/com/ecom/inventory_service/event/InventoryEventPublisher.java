package com.ecom.inventory_service.event;

import com.ecom.common_lib.events.InventorySeededEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.sns.topic.order-events-arn}")
    private String orderEventsTopicArn;

    public void publishInventoryReserved(InventoryReservedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            snsTemplate.send(orderEventsTopicArn,
                    MessageBuilder.withPayload(message)
                            .setHeader("eventType", "INVENTORY_RESERVED")
                            .setHeader("correlationId", event.getCorrelationId())
                            .build());
            log.info("Published INVENTORY_RESERVED for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_RESERVED", e);
            throw new RuntimeException(e);
        }
    }

    public void publishInventoryFailed(InventoryFailedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            snsTemplate.send(orderEventsTopicArn,
                    MessageBuilder.withPayload(message)
                            .setHeader("eventType", "INVENTORY_FAILED")
                            .setHeader("correlationId", event.getCorrelationId())
                            .build());
            log.info("Published INVENTORY_FAILED for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_FAILED", e);
            throw new RuntimeException(e);
        }
    }

    public void publishInventorySeeded(InventorySeededEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            snsTemplate.send(orderEventsTopicArn,
                    MessageBuilder.withPayload(message)
                            .setHeader("eventType", "INVENTORY_SEEDED")
                            .setHeader("correlationId", event.getCorrelationId())
                            .build());
            log.info("Published INVENTORY_SEEDED for productId: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_SEEDED", e);
            throw new RuntimeException(e);
        }
    }
}