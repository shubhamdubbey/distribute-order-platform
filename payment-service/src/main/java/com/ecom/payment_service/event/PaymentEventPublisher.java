package com.ecom.payment_service.event;

import com.ecom.common_lib.events.PaymentFailedEvent;
import com.ecom.common_lib.events.PaymentSuccessEvent;
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
public class PaymentEventPublisher {

    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.sns.topic.order-events-arn}")
    private String orderEventsTopicArn;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            snsTemplate.send(orderEventsTopicArn,
                    MessageBuilder.withPayload(message)
                            .setHeader("eventType", "PAYMENT_SUCCESS")
                            .setHeader("correlationId", event.getCorrelationId())
                            .build());
            log.info("Published PAYMENT_SUCCESS for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish PAYMENT_SUCCESS for orderId: {}",
                    event.getOrderId(), e);
            throw new RuntimeException(e);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            snsTemplate.send(orderEventsTopicArn,
                    MessageBuilder.withPayload(message)
                            .setHeader("eventType", "PAYMENT_FAILED")
                            .setHeader("correlationId", event.getCorrelationId())
                            .build());
            log.info("Published PAYMENT_FAILED for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish PAYMENT_FAILED for orderId: {}",
                    event.getOrderId(), e);
            throw new RuntimeException(e);
        }
    }
}