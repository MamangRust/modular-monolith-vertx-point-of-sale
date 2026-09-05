package io.example.role.domain.requests.role;

import lombok.Data;

@Data
public class FindAllRoles {
  private Integer page = 1;
  private Integer pageSize = 10;
  private String search = "";
}
