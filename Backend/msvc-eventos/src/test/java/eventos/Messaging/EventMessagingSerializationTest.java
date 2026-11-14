package eventos.Messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fija el contrato de wire que consume msvc-notificaciones (ver specs/comunicacion.md).
 * Si cambian nombres de campos o formatos de fecha, este test debe fallar.
 */
class EventMessagingSerializationTest {

    @Test
    void serializaElContratoDocumentado() {
        EventMessaging msg = new EventMessaging(
                "m-1",
                "e0000000-0000-4000-8000-000000000001",
                "EventoCancelado",
                "Rock in Rio",
                LocalDateTime.of(2026, 12, 20, 21, 0),
                null,
                Instant.parse("2026-11-13T19:20:00Z"));

        Message message = new JacksonJsonMessageConverter()
                .toMessage(msg, new MessageProperties());
        Map<?, ?> json = JsonMapper.builder().build().readValue(message.getBody(), Map.class);

        assertEquals("m-1", json.get("messageId"));
        assertEquals("e0000000-0000-4000-8000-000000000001", json.get("eventoId"));
        assertEquals("EventoCancelado", json.get("tipo"));
        assertEquals("Rock in Rio", json.get("nombre"));
        assertEquals("2026-12-20T21:00:00", json.get("fecha"));
        assertNull(json.get("cambios"));
        assertEquals("2026-11-13T19:20:00Z", json.get("occurredAt"));
    }

    @Test
    void conservaNanosegundosEnOccurredAt() {
        // Instant.now() produce precision de nanos; los mensajes reales lucen asi.
        Instant occurredAt = Instant.parse("2026-11-14T04:49:57.150570083Z");

        Message message = new JacksonJsonMessageConverter().toMessage(
                new EventMessaging("f894d182-cdbd-42ed-9fd2-10f29b8f949d",
                        "d0000000-0000-4000-8000-000000000001",
                        "EventoCancelado",
                        "Metallica — M72 World Tour",
                        LocalDateTime.of(2027, 3, 15, 21, 0),
                        null,
                        occurredAt),
                new MessageProperties());
        Map<?, ?> json = JsonMapper.builder().build().readValue(message.getBody(), Map.class);

        assertEquals("2027-03-15T21:00:00", json.get("fecha"));
        assertEquals("2026-11-14T04:49:57.150570083Z", json.get("occurredAt"));
    }
}
