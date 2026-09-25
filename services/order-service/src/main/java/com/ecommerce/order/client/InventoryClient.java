package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.ReleaseStockResponseDto;
import com.ecommerce.order.client.dto.ReservationItemDto;
import com.ecommerce.order.client.dto.ReserveStockResponseDto;

import java.util.List;
import java.util.UUID;

public interface InventoryClient {

    ReserveStockResponseDto reserveStock(UUID orderId, List<ReservationItemDto> items, Integer ttlMinutes);

    ReleaseStockResponseDto releaseStock(UUID orderId, String reason);
}
