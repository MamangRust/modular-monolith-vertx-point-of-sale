package io.example.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  @Mock
  private KafkaProducer<String, String> producer;

  @Test
  void sendMessageEncodesJsonAndPublishesRecord() {
    when(producer.send(any())).thenReturn(Future.succeededFuture(mock(RecordMetadata.class)));
    KafkaService service = new KafkaService(producer);
    JsonObject payload = new JsonObject().put("email", "a@b.c").put("body", "hi");

    Future<Void> result = service.sendMessage("email-service-topic-auth-register", "key-1", payload);

    assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : ""))
        .isTrue();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(KafkaProducerRecord.class);
    verify(producer).send(captor.capture());

    KafkaProducerRecord<String, String> record = captor.getValue();
    assertThat(record.topic()).isEqualTo("email-service-topic-auth-register");
    assertThat(record.key()).isEqualTo("key-1");
    assertThat(record.value()).contains("\"email\":\"a@b.c\"");
  }

  @Test
  void sendMessageRecoversWhenProducerFails() {
    when(producer.send(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("broker unreachable")));
    KafkaService service = new KafkaService(producer);

    Future<Void> result = service.sendMessage("topic", "k", new JsonObject().put("a", 1));

    // KafkaService recovers from producer failures silently (logs a warning)
    // so downstream services are not blocked by a transient Kafka outage.
    assertThat(result.succeeded()).isTrue();
  }
}
