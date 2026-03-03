package com.ecom.order_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqMonitorService {

    private final SqsClient sqsClient;

    @Value("${cloud.aws.sqs.dlq-url}")
    private String dlqUrl;

    // Runs every 60 seconds
//    @Scheduled(fixedDelay = 60000)
    public void monitorDlq() {
        try {
            // Check message count in DLQ
            GetQueueAttributesRequest attributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(dlqUrl)
                    .attributeNames(
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
                    )
                    .build();

            GetQueueAttributesResponse attributesResponse =
                    sqsClient.getQueueAttributes(attributesRequest);

            String messageCount = attributesResponse.attributes()
                    .get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);

            int count = Integer.parseInt(messageCount);

            if (count > 0) {
                log.error("ALERT: {} messages in DLQ. " +
                        "Immediate investigation required!", count);

                // Peek at messages without consuming them
                peekDlqMessages();
            } else {
                log.info("DLQ is clean. No failed messages.");
            }

        } catch (Exception e) {
            log.error("Failed to monitor DLQ", e);
        }
    }

    private void peekDlqMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(5)
                    .visibilityTimeout(0) // Don't hide messages from other consumers
                    .messageAttributeNames("All")
                    .build();

            List<Message> messages = sqsClient.receiveMessage(receiveRequest)
                    .messages();

            messages.forEach(msg -> {
                log.error("DLQ Message - MessageId: {}, Body preview: {}",
                        msg.messageId(),
                        msg.body().substring(0, Math.min(200, msg.body().length()))
                );
            });

        } catch (Exception e) {
            log.error("Failed to peek DLQ messages", e);
        }
    }
}