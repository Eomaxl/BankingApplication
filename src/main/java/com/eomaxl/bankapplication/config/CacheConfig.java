package com.eomaxl.bankapplication.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!aws")
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean
    @Profile("aws")
    public CacheManager redisCacheManager() {
        // Redis Configuration would come here for the production env
        // Using redis for distributed caching across multiple instances
        return new ConcurrentMapCacheManager(); // PlaceHolder
    }
}
