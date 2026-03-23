package com.ecom.inventory_service.consumer;

import com.ecom.common_lib.events.OrderCreatedEvent;
import com.ecom.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @SqsListener("inventory-queue")
    public void handleOrderCreated(Message<String> message) {

        String correlationId = null;

        try {
            // Extract correlationId from message attributes
            correlationId = (String) message.getHeaders().get("correlationId");
            if (correlationId == null) {
                correlationId = java.util.UUID.randomUUID().toString();
            }

            // Initialize MDC for this thread
            MDC.put("correlationId", correlationId);

            log.info("Received OrderCreatedEvent from SQS");

            // SNS wraps message in envelope — unwrap it
            String body = message.getPayload();
            String actualMessage = extractMessageFromSnsEnvelope(body);

            OrderCreatedEvent event = objectMapper.readValue(
                    actualMessage, OrderCreatedEvent.class);

            inventoryService.processOrderCreated(event, correlationId);

        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent", e);
            throw new RuntimeException(e);

        } finally {
            MDC.clear();
        }
    }

    private String extractMessageFromSnsEnvelope(String body) throws Exception {
        // SNS wraps message like: {"Type":"Notification","Message":"actual-json"}
        com.fasterxml.jackson.databind.JsonNode node =
                objectMapper.readTree(body);

        if (node.has("Message")) {
            // This is SNS envelope — extract inner message
            return node.get("Message").asText();
        }

        // Direct SQS message — return as is
        return body;
    }
}