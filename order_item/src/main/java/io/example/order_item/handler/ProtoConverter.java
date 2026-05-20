package io.example.order_item.handler;

import com.google.protobuf.StringValue;
import io.example.order_item.model.OrderItemResponse;
import io.example.order_item.model.OrderItemResponseDeleteAt;

public class ProtoConverter {

    public static pb.order_item.OrderItem.OrderItemResponse fromOrderItemResponse(OrderItemResponse r) {
        if (r == null) return pb.order_item.OrderItem.OrderItemResponse.getDefaultInstance();
        return pb.order_item.OrderItem.OrderItemResponse.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setOrderId(r.getOrderId() != null ? r.getOrderId().intValue() : 0)
                .setProductId(r.getProductId() != null ? r.getProductId().intValue() : 0)
                .setQuantity(r.getQuantity() != null ? r.getQuantity() : 0)
                .setPrice(r.getPrice() != null ? r.getPrice() : 0)
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    public static pb.order_item.OrderItem.OrderItemResponseDeleteAt fromOrderItemResponseDeleteAt(OrderItemResponseDeleteAt r) {
        if (r == null) return pb.order_item.OrderItem.OrderItemResponseDeleteAt.getDefaultInstance();
        pb.order_item.OrderItem.OrderItemResponseDeleteAt.Builder b = pb.order_item.OrderItem.OrderItemResponseDeleteAt.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setOrderId(r.getOrderId() != null ? r.getOrderId().intValue() : 0)
                .setProductId(r.getProductId() != null ? r.getProductId().intValue() : 0)
                .setQuantity(r.getQuantity() != null ? r.getQuantity() : 0)
                .setPrice(r.getPrice() != null ? r.getPrice() : 0)
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");

        if (r.getDeletedAt() != null) {
            b.setDeletedAt(StringValue.of(r.getDeletedAt()));
        }
        return b.build();
    }
}
