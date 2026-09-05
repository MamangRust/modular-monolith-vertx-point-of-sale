package io.example.category.domain.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateCategoryRequest {
    @JsonProperty("category_id")
    private Integer categoryId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("slug_category")
    private String slugCategory;
}
