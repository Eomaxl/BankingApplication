package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Transaction;
import com.eomaxl.bankapplication.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Process transactions in batches for high throughput
     * Uses Hibernate batch processing for optimal performance
     */
    @Async("transactionExecutor")
    @Transactional
    public CompletableFuture<Void> processBatchTransactions(List<Transaction> transactions) {
        log.info("Processing batch of {} transactions", transactions.size());

        try {
            // Hibernate will batch these inserts automatically due to our configuration
            transactionRepository.saveAll(transactions);

            log.info("Successfully processed batch of {} transactions", transactions.size());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to process transaction batch", e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    /**
     * Async notification processing to avoid blocking main transaction flow
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendTransactionNotifications(List<Transaction> transactions) {
        log.info("Sending notifications for {} transactions", transactions.size());

        // Process notifications asynchronously
        transactions.parallelStream().forEach(transaction -> {
            try {
                // Send notification logic here
                log.debug("Notification sent for transaction: {}", transaction.getTransactionId());
            } catch (Exception e) {
                log.warn("Failed to send notification for transaction: {}", transaction.getTransactionId(), e);
            }
        });

        return CompletableFuture.completedFuture(null);
    }
}
