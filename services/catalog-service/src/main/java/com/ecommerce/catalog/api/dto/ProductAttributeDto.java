package com.ecommerce.catalog.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductAttributeDto {

    private UUID id;
    private String name;
    private List<ProductAttributeValueDto> values = new ArrayList<>();

    public ProductAttributeDto() {
    }

    public ProductAttributeDto(UUID id, String name, List<ProductAttributeValueDto> values) {
        this.id = id;
        this.name = name;
        this.values = values != null ? values : new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProductAttributeValueDto> getValues() {
        return values;
    }

    public void setValues(List<ProductAttributeValueDto> values) {
        this.values = values != null ? values : new ArrayList<>();
    }
}
