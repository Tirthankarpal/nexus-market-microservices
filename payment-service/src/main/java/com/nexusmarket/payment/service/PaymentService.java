package com.nexusmarket.payment.service;

import com.nexusmarket.payment.event.PaymentCompletedEvent;
import com.nexusmarket.payment.event.PaymentRequiredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class PaymentService {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final Random random = new Random();

    public PaymentService(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment-required", groupId = "payment-group")
    public void processPayment(PaymentRequiredEvent event) {
        log.info("Processing payment for order {} for user {} for amount {}", 
                event.orderNumber(), event.userEmail(), event.amount());
        
        try {
            // Simulate payment processing time
            Thread.sleep(2000);
            
            // Mock payment logic: 80% chance of success for educational purposes
            boolean isSuccess = random.nextInt(100) < 80;
            String message = isSuccess ? "Payment processed successfully" : "Insufficient funds (simulated)";
            
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
