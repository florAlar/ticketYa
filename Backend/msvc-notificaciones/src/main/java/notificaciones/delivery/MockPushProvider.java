package notificaciones.delivery;

import java.util.UUID;
import notificaciones.entity.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envío push simulado (sin Firebase ni proveedor externo).
 *
 * <p>Registra el aviso y devuelve un id de mensaje ficticio. El objetivo es ejercitar el flujo
 * completo (PENDIENTE → intento → ENVIADA) sin credenciales ni red externa.
 */
@Component
public class MockPushProvider implements NotificacionProvider {

    public static final String CANAL = "PUSH";
    private static final Logger log = LoggerFactory.getLogger(MockPushProvider.class);

    @Override
    public String canal() {
        return CANAL;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        String mensajeId = "mock-push-" + UUID.randomUUID();
        log.info("[PUSH-MOCK] id={} usuario={} asunto='{}' contenido='{}'",
                mensajeId,
                notificacion.getIdUsuario(),
                notificacion.getAsunto(),
                notificacion.getContenido());
        return ResultadoEnvio.ok(mensajeId);
    }
}
