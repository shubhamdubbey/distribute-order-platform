package com.ecom.order_service.event;

import com.ecom.common_lib.events.OrderCreatedEvent;
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
public class OrderEventPublisher {

    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.sns.topic.order-events-arn}")
    private String orderEventsTopicArn;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);

            snsTemplate.send(orderEventsTopicArn,
                    MessageBuilder.withPayload(message)
                            .setHeader("eventType", "ORDER_CREATED")
                            .build());

            log.info("Published ORDER_CREATED event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish ORDER_CREATED event for orderId: {}",
                    event.getOrderId(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
}