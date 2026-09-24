package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class OrderTransitionRequest {

    @NotNull(message = "Target status is required")
    private OrderStatus targetStatus;

    private UUID actorId;

    private String note;

    public OrderTransitionRequest() {
    }

    public OrderTransitionRequest(OrderStatus targetStatus, UUID actorId, String note) {
        this.targetStatus = targetStatus;
        this.actorId = actorId;
        this.note = note;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(OrderStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
