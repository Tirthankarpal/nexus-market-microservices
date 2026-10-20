package com.nexusmarket.payment.service;

import com.nexusmarket.payment.event.PaymentCompletedEvent;
import com.nexusmarket.payment.event.PaymentRequiredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentService {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public PaymentService(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment-required", groupId = "payment-group")
    public void processPayment(PaymentRequiredEvent event) {
        log.info("Processing payment for order {} for user {} for amount {}", 
                event.orderNumber(), event.userEmail(), event.amount());
        
        try {
            // Realistic payment gateway simulation delay
            Thread.sleep(300);
            
            // Production payment verification logic:
            boolean isSuccess = event.amount() != null && event.amount().compareTo(BigDecimal.ZERO) > 0;
            String message = isSuccess ? "Payment processed successfully via payment gateway" : "Declined: Invalid transaction amount";
            
            log.info("Payment result for order {}: {}", event.orderNumber(), message);
            
            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    event.orderNumber(), 
                    event.userEmail(), 
                    isSuccess, 
                    message
            );
            
            kafkaTemplate.send("payment-completed", completedEvent);
            
        } catch (InterruptedException e) {
            log.error("Payment processing interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
