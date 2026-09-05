package io.example.order.domain.requests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OrderRequestTest {

  @Test
  void createOrderRequest_shouldBuild() {
    CreateOrderItemRequest item = CreateOrderItemRequest.builder()
        .productId(1L)
        .quantity(2)
        .build();

    CreateOrderRequest request = CreateOrderRequest.builder()
        .merchantId(1)
        .cashierId(1)
        .items(List.of(item))
        .build();

    assertThat(request.getMerchantId()).isEqualTo(1);
    assertThat(request.getCashierId()).isEqualTo(1);
    assertThat(request.getItems()).hasSize(1);

    CreateOrderItemRequest actualItem = request.getItems().get(0);
    assertThat(actualItem.getProductId()).isEqualTo(1L);
    assertThat(actualItem.getQuantity()).isEqualTo(2);
  }

  @Test
  void createOrderRequest_shouldAllowEmptyItems() {
    CreateOrderRequest request = CreateOrderRequest.builder()
        .merchantId(1)
        .cashierId(1)
        .items(List.of())
        .build();

    assertThat(request.getMerchantId()).isEqualTo(1);
    assertThat(request.getCashierId()).isEqualTo(1);
    assertThat(request.getItems()).isEmpty();
  }

  @Test
  void createOrderItemRequest_shouldBuild() {
    CreateOrderItemRequest item = CreateOrderItemRequest.builder()
        .productId(10L)
        .quantity(3)
        .build();

    assertThat(item.getProductId()).isEqualTo(10L);
    assertThat(item.getQuantity()).isEqualTo(3);
  }

  @Test
  void createOrderItemRequest_shouldUseJsonPropertyNames() {
    CreateOrderItemRequest item = new CreateOrderItemRequest();
    item.setProductId(5L);
    item.setQuantity(1);

    assertThat(item.getProductId()).isEqualTo(5L);
    assertThat(item.getQuantity()).isEqualTo(1);
  }

  @Test
  void findAllOrders_shouldUseDefaults() {
    FindAllOrders request = new FindAllOrders();

    assertThat(request.getPage()).isEqualTo(0);
    assertThat(request.getPageSize()).isEqualTo(0);
    assertThat(request.getSearch()).isNull();
  }

  @Test
  void findAllOrders_shouldSetFields() {
    FindAllOrders request = new FindAllOrders();
    request.setPage(2);
    request.setPageSize(20);
    request.setSearch("test");

    assertThat(request.getPage()).isEqualTo(2);
    assertThat(request.getPageSize()).isEqualTo(20);
    assertThat(request.getSearch()).isEqualTo("test");
  }
}
