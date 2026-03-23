package com.ecom.order_service.consumer;


import com.ecom.common_lib.events.*;
import com.ecom.order_service.entity.OrderStatus;
import com.ecom.order_service.service.InventoryRedisService;
import com.ecom.order_service.service.OrderService;
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
public class OrderUpdateEventConsumer {

    private final InventoryRedisService inventoryRedisService;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

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
                    InventoryReservedEvent event = objectMapper.readValue(
                            actualMessage, InventoryReservedEvent.class);
                    boolean result = orderService.updateOrderStatus(
                            event.getOrderId(), OrderStatus.PAYMENT_PROCESSING, event.getAmount());
                    if (!result) {
                        log.error("Failed to update order status in reserved inventory flow for orderId: {}",
                                event.getOrderId());
                    }
                }
                case "INVENTORY_FAILED" -> {
                    InventoryFailedEvent event = objectMapper.readValue(
                            actualMessage, InventoryFailedEvent.class);
                    boolean result = orderService.handleInventoryFailed(
                            event.getOrderId(), event.getProductId(), event.getQuantity());
                    if (!result) {
                        log.error("Failed to handle inventory failure for orderId: {}",
                                event.getOrderId());
                    }
                }
                case "PAYMENT_SUCCESS" -> {
                    PaymentSuccessEvent event = objectMapper.readValue(
                            actualMessage, PaymentSuccessEvent.class);
                    boolean result = orderService.updateOrderStatus(
                            event.getOrderId(), OrderStatus.CONFIRMED, event.getAmount());
                    if (!result) {
                        log.error("Failed to update order status in payment success flow for orderId: {}",
                                event.getOrderId());
                    }
                }
                case "PAYMENT_FAILED" -> {
                    PaymentFailedEvent event = objectMapper.readValue(
                            actualMessage, PaymentFailedEvent.class);
                    boolean result = orderService.handlePaymentFailed(
                            event.getOrderId());
                    if (!result) {
                        log.error("Failed to handle payment failure for orderId: {}",
                                event.getOrderId());
                    }
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