package com.eomaxl.bankapplication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.eomaxl.bankapplication.repository")
@EnableTransactionManagement
public class RepositoryConfig {
    // Repository configuration
}
