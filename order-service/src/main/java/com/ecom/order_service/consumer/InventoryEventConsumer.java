package com.ecom.order_service.consumer;


import com.ecom.common_lib.events.InventorySeededEvent;
import com.ecom.order_service.service.InventoryRedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryRedisService inventoryRedisService;
    private final ObjectMapper objectMapper;

    @SqsListener("order-updates-queue")
    public void handleInventoryEvent(Message<String> message) {

        String correlationId = null;

        try {
            String body = message.getPayload();
            JsonNode envelope = objectMapper.readTree(body);

            // Extract eventType and correlationId from SNS envelope
            // MessageAttributes section inside the SQS message body
            String eventType = extractFromEnvelope(envelope, "eventType");
            correlationId = extractFromEnvelope(envelope, "correlationId");

            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put("correlationId", correlationId);

            log.info("Received event of type: {}", eventType);

            if (eventType == null) {
                log.warn("eventType is null, skipping message");
                return;
            }

            // Extract actual message from SNS envelope
            String actualMessage = envelope.has("Message")
                    ? envelope.get("Message").asText()
                    : body;

            switch (eventType) {
                case "INVENTORY_SEEDED" -> {
                    InventorySeededEvent event = objectMapper.readValue(
                            actualMessage, InventorySeededEvent.class);
                    handleInventorySeeded(event);
                }
                case "INVENTORY_RESERVED" -> {
                    log.info("Inventory reserved — order status update coming soon");
                    // Will implement when we build order status update flow
                }
                case "INVENTORY_FAILED" -> {
                    log.info("Inventory failed — order status update coming soon");
                    // Will implement when we build order status update flow
                }
                default -> log.warn("Unknown event type: {}, skipping", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process event", e);
            throw new RuntimeException(e);

        } finally {
            MDC.clear();
        }
    }

    private void handleInventorySeeded(InventorySeededEvent event) {
        log.info("Syncing Redis inventory for productId: {}", event.getProductId());

        inventoryRedisService.seedInventory(
                event.getProductId().toString(),
                event.getQuantity()
        );

        log.info("Redis synced for productId: {}. Quantity: {}",
                event.getProductId(), event.getQuantity());
    }

    // Extract value from SNS MessageAttributes inside envelope
    private String extractFromEnvelope(JsonNode envelope, String attributeName) {
        try {
            if (envelope.has("MessageAttributes")) {
                JsonNode attributes = envelope.get("MessageAttributes");
                if (attributes.has(attributeName)) {
                    return attributes.get(attributeName)
                            .get("Value")
                            .asText();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract {} from envelope", attributeName);
        }
        return null;
    }
}