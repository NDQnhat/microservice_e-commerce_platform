package com.ecommerce.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryRedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(InventoryRedisCacheService.class);
    private static final String KEY_PREFIX = "stock:sku:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public InventoryRedisCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<Integer> getCachedAvailableStock(UUID skuId) {
        if (skuId == null) return Optional.empty();
        try {
            String val = redisTemplate.opsForValue().get(KEY_PREFIX + skuId);
            if (val != null) {
                return Optional.of(Integer.parseInt(val));
            }
        } catch (Exception e) {
            log.debug("Redis read failed for SKU stock {}: {}", skuId, e.getMessage());
        }
        return Optional.empty();
    }

    public void cacheAvailableStock(UUID skuId, int available) {
        if (skuId == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + skuId, String.valueOf(available), TTL);
        } catch (Exception e) {
            log.debug("Redis write failed for SKU stock {}: {}", skuId, e.getMessage());
        }
    }

    public void evict(UUID skuId) {
        if (skuId == null) return;
        try {
            redisTemplate.delete(KEY_PREFIX + skuId);
        } catch (Exception e) {
            log.debug("Redis eviction failed for SKU stock {}: {}", skuId, e.getMessage());
        }
    }

    public void evictAll(Collection<UUID> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return;
        try {
            for (UUID skuId : skuIds) {
                if (skuId != null) {
                    redisTemplate.delete(KEY_PREFIX + skuId);
                }
            }
        } catch (Exception e) {
            log.debug("Redis bulk eviction failed: {}", e.getMessage());
        }
    }
}
