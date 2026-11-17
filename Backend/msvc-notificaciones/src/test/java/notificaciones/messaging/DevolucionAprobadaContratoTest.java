package notificaciones.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrato de wire con msvc-entradas (ver specs/comunicacion.md).
 * Si el productor cambia nombres o formatos, este test debe fallar.
 */
class DevolucionAprobadaContratoTest {

    private static final String JSON = """
            {
              "messageId": "m-4",
              "devolucionId": "d0000000-0000-4000-8000-000000000001",
              "entradaId": "e0000000-0000-4000-8000-000000000001",
              "clienteId": "b0000000-0000-4000-8000-000000000001",
              "eventoId": "ev000000-0000-4000-8000-000000000001",
              "monto": 95000.00,
              "moneda": "ARS",
              "occurredAt": "2026-11-17T12:00:00Z"
            }
            """;

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void deserializaElPayloadPublicadoPorMsvcEntradas() {
        DevolucionAprobadaDTO dto = mapper.readValue(JSON, DevolucionAprobadaDTO.class);

        assertEquals("m-4", dto.messageId());
        assertEquals("d0000000-0000-4000-8000-000000000001", dto.devolucionId());
        assertEquals("e0000000-0000-4000-8000-000000000001", dto.entradaId());
        assertEquals("b0000000-0000-4000-8000-000000000001", dto.clienteId());
        assertEquals("ev000000-0000-4000-8000-000000000001", dto.eventoId());
        assertEquals(new BigDecimal("95000.00"), dto.monto());
        assertEquals("ARS", dto.moneda());
        assertEquals(Instant.parse("2026-11-17T12:00:00Z"), dto.occurredAt());
    }
}
