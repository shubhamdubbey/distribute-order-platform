package com.ecom.inventory_service.consumer;

import com.ecom.common_lib.events.PaymentFailedEvent;
import com.ecom.common_lib.events.PaymentSuccessEvent;
import com.ecom.inventory_service.service.InventoryService;
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
public class PaymentEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @SqsListener("inventory-payment-queue")
    public void handlePaymentEvent(Message<String> message) {

        String correlationId = null;

        try {
            String body = message.getPayload();
            JsonNode envelope = objectMapper.readTree(body);

            String eventType = extractFromEnvelope(envelope, "eventType");
            correlationId = extractFromEnvelope(envelope, "correlationId");

            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put("correlationId", correlationId);

            log.info("Received payment event of type: {}", eventType);

            if (eventType == null) {
                log.warn("eventType is null, skipping message");
                return;
            }

            String actualMessage = envelope.has("Message")
                    ? envelope.get("Message").asText()
                    : body;

            switch (eventType) {
                    case "PAYMENT_SUCCESS" -> {
                        PaymentSuccessEvent event = objectMapper.readValue(
                                actualMessage, PaymentSuccessEvent.class);
                        inventoryService.handlePaymentSuccess(event);
                    }
                    case "PAYMENT_FAILED" -> {
                        PaymentFailedEvent event = objectMapper.readValue(
                                actualMessage, PaymentFailedEvent.class);
                        inventoryService.handlePaymentFailed(event);
                    }
                    default -> log.warn("Unhandled event type: {} in payment consumer",
                            eventType);
            }


        } catch (Exception e) {
            log.error("Failed to process payment event", e);
            throw new RuntimeException(e);

        } finally {
            MDC.clear();
        }
    }

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