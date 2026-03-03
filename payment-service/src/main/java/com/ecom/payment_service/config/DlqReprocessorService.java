package com.ecom.payment_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReprocessorService {

    private final SqsClient sqsClient;

    @Value("${cloud.aws.sqs.dlq-url}")
    private String dlqUrl;

    public int reprocessMessages(String targetQueueUrl) {
        int reprocessedCount = 0;

        try {
            // Keep reading until DLQ is empty
            while (true) {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(dlqUrl)
                        .maxNumberOfMessages(10)
                        .visibilityTimeout(30)
                        .messageAttributeNames("All")
                        .build();

                List<Message> messages = sqsClient
                        .receiveMessage(receiveRequest)
                        .messages();

                if (messages.isEmpty()) {
                    break;
                }

                for (Message message : messages) {
                    try {
                        // Send to target queue for reprocessing
                        SendMessageRequest sendRequest = SendMessageRequest.builder()
                                .queueUrl(targetQueueUrl)
                                .messageBody(message.body())
                                .build();

                        sqsClient.sendMessage(sendRequest);

                        // Delete from DLQ after successful requeue
                        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                                .queueUrl(dlqUrl)
                                .receiptHandle(message.receiptHandle())
                                .build();

                        sqsClient.deleteMessage(deleteRequest);

                        reprocessedCount++;
                        log.info("Reprocessed message: {}", message.messageId());

                    } catch (Exception e) {
                        log.error("Failed to reprocess message: {}",
                                message.messageId(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("DLQ reprocessing failed", e);
        }

        log.info("Reprocessed {} messages from DLQ", reprocessedCount);
        return reprocessedCount;
    }
}