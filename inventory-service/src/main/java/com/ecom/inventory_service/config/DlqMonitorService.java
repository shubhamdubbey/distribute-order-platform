package com.ecom.inventory_service.config;

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

    @Value("${cloud.aws.sqs.inventory-dlq-url}")
    private String inventoryDlqUrl;

    @Value("${cloud.aws.sqs.inventory-payment-dlq-url}")
    private String inventoryPaymentDlqUrl;

    @Scheduled(fixedDelay = 60000)
    public void monitorDlqs() {
        checkDlq(inventoryDlqUrl, "inventory-service-dlq");
        checkDlq(inventoryPaymentDlqUrl, "inventory-payment-dlq");
    }

    private void checkDlq(String dlqUrl, String dlqName) {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(dlqUrl)
                    .attributeNames(
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build();

            String messageCount = sqsClient.getQueueAttributes(request)
                    .attributes()
                    .get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);

            int count = Integer.parseInt(messageCount);

            if (count > 0) {
                log.error("ALERT: {} messages in {}. " +
                        "Immediate investigation required!", count, dlqName);
                peekDlqMessages(dlqUrl, dlqName);
            } else {
                log.info("DLQ {} is clean.", dlqName);
            }

        } catch (Exception e) {
            log.error("Failed to monitor DLQ: {}", dlqName, e);
        }
    }

    private void peekDlqMessages(String dlqUrl, String dlqName) {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(5)
                    .visibilityTimeout(0)
                    .messageAttributeNames("All")
                    .build();

            List<Message> messages = sqsClient
                    .receiveMessage(receiveRequest)
                    .messages();

            messages.forEach(msg ->
                log.error("DLQ [{}] Message - MessageId: {}, Body: {}",
                        dlqName,
                        msg.messageId(),
                        msg.body().substring(0, Math.min(200, msg.body().length())))
            );

        } catch (Exception e) {
            log.error("Failed to peek DLQ: {}", dlqName, e);
        }
    }
}