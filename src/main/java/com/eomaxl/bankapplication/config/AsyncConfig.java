package com.eomaxl.bankapplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "transactionExecutor")
    public Executor transactionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // High throughput configuration
        executor.setCorePoolSize(20);           // Base threads for transaction processing
        executor.setMaxPoolSize(100);           // Max threads during peak load
        executor.setQueueCapacity(1000);        // Queue for pending transactions
        executor.setKeepAliveSeconds(60);       // Thread keep-alive time
        executor.setThreadNamePrefix("TxnAsync-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Separate pool for notifications to avoid blocking transactions
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("NotifyAsync-");

        executor.initialize();
        return executor;
    }
}