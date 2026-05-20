package io.example.category.domain.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
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
