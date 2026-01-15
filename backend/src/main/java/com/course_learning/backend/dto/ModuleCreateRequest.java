package com.course_learning.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public class ModuleCreateRequest {
    @NotBlank(message = "Module title is required")
    private String title;

    private String description;

    @Min(value = 0, message = "Order index must be non-negative")
    private Integer orderIndex;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
