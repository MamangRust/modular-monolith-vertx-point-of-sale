package io.example.common.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedResult<T> {
  private List<T> data;
  private int totalRecords;
}
