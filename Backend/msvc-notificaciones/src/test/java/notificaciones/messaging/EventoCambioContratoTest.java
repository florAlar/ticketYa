package notificaciones.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrato de wire con msvc-eventos (ver specs/comunicacion.md).
 * Si el productor cambia nombres o formatos, este test debe fallar.
 */
class EventoCambioContratoTest {

    private static final String JSON = """
            {
              "messageId": "m-1",
              "eventoId": "e0000000-0000-4000-8000-000000000001",
              "tipo": "EventoCancelado",
              "nombre": "Rock in Rio",
              "fecha": "2026-12-20T21:00:00",
              "cambios": null,
              "occurredAt": "2026-11-13T19:20:00Z"
            }
            """;

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void deserializaElPayloadPublicadoPorMsvcEventos() {
        EventoCambioDTO dto = mapper.readValue(JSON, EventoCambioDTO.class);

        assertEquals("m-1", dto.messageId());
        assertEquals("e0000000-0000-4000-8000-000000000001", dto.eventoId());
        assertEquals("EventoCancelado", dto.tipo());
        assertEquals("Rock in Rio", dto.nombre());
        assertEquals(LocalDateTime.of(2026, 12, 20, 21, 0), dto.fecha());
        assertNull(dto.cambios());
        assertEquals(Instant.parse("2026-11-13T19:20:00Z"), dto.occurredAt());
    }

    @Test
    void deserializaUnMensajeRealCapturadoDelBroker() {
        // Captura real del panel de RabbitMQ (ms-eventos -> evento.cancelado).
        String json = """
                {"messageId":"f894d182-cdbd-42ed-9fd2-10f29b8f949d",
                 "eventoId":"d0000000-0000-4000-8000-000000000001",
                 "tipo":"EventoCancelado",
                 "nombre":"Metallica — M72 World Tour",
                 "fecha":"2027-03-15T21:00:00",
                 "cambios":null,
                 "occurredAt":"2026-11-14T04:49:57.150570083Z"}
                """;

        EventoCambioDTO dto = mapper.readValue(json, EventoCambioDTO.class);

        assertEquals("f894d182-cdbd-42ed-9fd2-10f29b8f949d", dto.messageId());
        assertEquals("d0000000-0000-4000-8000-000000000001", dto.eventoId());
        assertEquals("Metallica — M72 World Tour", dto.nombre());
        assertEquals(LocalDateTime.of(2027, 3, 15, 21, 0), dto.fecha());
        assertEquals(Instant.parse("2026-11-14T04:49:57.150570083Z"), dto.occurredAt());
    }
}
