package com.ecommerce.fulfillment.domain.model;

public enum ShipmentStatus {
    PACKING,
    SHIPPED,
    DELIVERED;

    public boolean canTransitionTo(ShipmentStatus target) {
        if (this == PACKING && target == SHIPPED) {
            return true;
        }
        if (this == SHIPPED && target == DELIVERED) {
            return true;
        }
        return false;
    }
}
