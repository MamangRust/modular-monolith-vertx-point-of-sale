package io.example.apigateway.utils;

import com.google.protobuf.MessageOrBuilder;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtoMapperTest {

    @Mock
    MessageOrBuilder protoMock;

    @Test
    void toJson_shouldReturnEmptyObjectForNullProto() {
        JsonObject result = ProtoMapper.toJson(null);
        assertThat(result).isEqualTo(new JsonObject());
    }

    @Test
    void toJson_shouldReturnJsonForValidProto() {
        // Use Struct (google.protobuf.Struct) — maps cleanly to JSON objects
        var struct = com.google.protobuf.Struct.newBuilder()
                .putFields("key", com.google.protobuf.Value.newBuilder().setStringValue("val").build())
                .build();

        JsonObject result = ProtoMapper.toJson(struct);

        // Struct serializes as a plain JSON object per protobuf JSON format
        assertThat(result).isNotNull();
        assertThat(result.getString("key")).isEqualTo("val");
    }

    @Test
    void toJson_shouldReturnErrorObjectWhenProtoThrows() {
        when(protoMock.getDescriptorForType()).thenThrow(new RuntimeException("mock failure"));

        JsonObject result = ProtoMapper.toJson(protoMock);
        assertThat(result.getString("error")).contains("Failed to serialize protobuf");
    }
}
