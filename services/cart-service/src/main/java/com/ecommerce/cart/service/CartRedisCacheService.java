package com.ecommerce.cart.service;

import com.ecommerce.cart.api.dto.CartDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartRedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(CartRedisCacheService.class);
    private static final String CART_CACHE_PREFIX = "cart:customer:";

    private final RedisTemplate<String, Object> redisTemplate;

    public CartRedisCacheService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<CartDto> getCachedCart(UUID customerId) {
        if (redisTemplate == null || customerId == null) {
            return Optional.empty();
        }
        try {
            Object cached = redisTemplate.opsForValue().get(CART_CACHE_PREFIX + customerId);
            if (cached instanceof CartDto) {
                return Optional.of((CartDto) cached);
            }
        } catch (Exception ex) {
            log.debug("Redis cart cache read skipped: {}", ex.getMessage());
        }
        return Optional.empty();
    }

    public void cacheCart(CartDto cart) {
        if (redisTemplate == null || cart == null || cart.getCustomerId() == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(CART_CACHE_PREFIX + cart.getCustomerId(), cart, Duration.ofHours(24));
        } catch (Exception ex) {
            log.debug("Redis cart cache write skipped: {}", ex.getMessage());
        }
    }

    public void evictCart(UUID customerId) {
        if (redisTemplate == null || customerId == null) {
            return;
        }
        try {
            redisTemplate.delete(CART_CACHE_PREFIX + customerId);
        } catch (Exception ex) {
            log.debug("Redis cart cache evict skipped: {}", ex.getMessage());
        }
    }
}
