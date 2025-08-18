package com.eomaxl.bankapplication.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public Timer transactionTimer(MeterRegistry registry) {
        return Timer.builder("banking.transaction.duration")
                .description("Time taken to process banking transactions")
                .register(registry);
    }

    @Bean
    public Timer transferTimer(MeterRegistry registry) {
        return Timer.builder("banking.transfer.duration")
                .description("Time taken to process money transfers")
                .register(registry);
    }

    @Bean
    public Timer databaseTimer(MeterRegistry registry) {
        return Timer.builder("banking.database.query.duration")
                .description("Time taken for database queries")
                .register(registry);
    }
}