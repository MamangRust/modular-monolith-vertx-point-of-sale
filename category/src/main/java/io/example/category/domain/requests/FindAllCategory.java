package io.example.category.domain.requests;

import lombok.Data;

@Data
public class FindAllCategory {
    private String search;
    private Integer page;
    private Integer pageSize;
}
