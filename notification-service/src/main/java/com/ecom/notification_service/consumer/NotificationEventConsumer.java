package com.ecom.notification_service.consumer;

import com.ecom.common_lib.events.OrderCreatedEvent;
import com.ecom.common_lib.events.PaymentFailedEvent;
import com.ecom.notification_service.service.EmailService;
import com.ecom.notification_service.template.EmailTemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final ObjectMapper objectMapper;

    @Value("${notification.ses.test-recipient-email}")
    private String testRecipientEmail;

    @SqsListener("${spring.cloud.aws.sqs.notification-queue-url}")
    public void handleEvent(String rawMessage) {
        try {
            // Parse SNS envelope
            JsonNode snsEnvelope = objectMapper.readTree(rawMessage);

            // Get the actual message body
            String messageBody = snsEnvelope.has("Message")
                    ? snsEnvelope.get("Message").asText()
                    : rawMessage;

            // Get eventType from MessageAttributes (where it actually lives)
            String eventType = null;
            if (snsEnvelope.has("MessageAttributes")) {
                JsonNode attrs = snsEnvelope.get("MessageAttributes");
                if (attrs.has("eventType")) {
                    eventType = attrs.get("eventType").get("Value").asText();
                }
            }

            // Fallback: try reading eventType from message body itself
            if (eventType == null) {
                JsonNode bodyNode = objectMapper.readTree(messageBody);
                if (bodyNode.has("eventType")) {
                    eventType = bodyNode.get("eventType").asText();
                }
            }

            if (eventType == null) {
                log.warn("No eventType found, skipping message: {}", rawMessage);
                return;
            }

            log.info("Notification received | eventType={}", eventType);

            JsonNode messageNode = objectMapper.readTree(messageBody);

            switch (eventType) {
                case "ORDER_PLACED" -> {
                    OrderCreatedEvent event = objectMapper.treeToValue(messageNode, OrderCreatedEvent.class);
                    sendOrderConfirmation(event);
                }
                case "PAYMENT_FAILED" -> {
                    PaymentFailedEvent event = objectMapper.treeToValue(messageNode, PaymentFailedEvent.class);
                    sendPaymentFailedNotification(event);
                }
                default -> log.warn("Unhandled eventType: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process notification event | raw={}", rawMessage, e);
            throw new RuntimeException(e);
        }
    }

    private void sendOrderConfirmation(OrderCreatedEvent event) {
        log.info("Sending order confirmation | orderId={}", event.getOrderId());

        String subject = "✅ Order Confirmed - #" + event.getOrderId();
        String html = emailTemplateService.buildOrderPlacedEmail(
                event.getOrderId().toString(),
                event.getProductId().toString(),
                event.getQuantity(),
                event.getAmount()
        );

        emailService.sendEmail(testRecipientEmail, subject, html);
        log.info("Order confirmation email sent | orderId={}", event.getOrderId());
    }

    private void sendPaymentFailedNotification(PaymentFailedEvent event) {
        log.info("Sending payment failed email | orderId={}", event.getOrderId());

        String subject = "❌ Payment Failed - Order #" + event.getOrderId();
        String html = emailTemplateService.buildPaymentFailedEmail(
                event.getOrderId().toString(),
                event.getProductId().toString(),
                event.getQuantity(),
                event.getAmount()
        );

        emailService.sendEmail(testRecipientEmail, subject, html);
        log.info("Payment failed email sent | orderId={}", event.getOrderId());
    }
}