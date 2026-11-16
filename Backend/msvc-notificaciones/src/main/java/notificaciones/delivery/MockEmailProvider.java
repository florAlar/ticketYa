package notificaciones.delivery;

import java.util.UUID;
import notificaciones.entity.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Envío de email simulado. No habla SMTP: sirve para las plantillas con canal EMAIL
 * (por ejemplo {@code CompraConfirmada}) sin sumar infraestructura de correo.
 *
 * <p>Se activa con {@code ticketya.notificaciones.email.provider=mock} (default). En Docker
 * Compose se usa {@code smtp} + MailHog; ver {@link SmtpEmailProvider}.
 */
@Component
@ConditionalOnProperty(
        name = "ticketya.notificaciones.email.provider",
        havingValue = "mock",
        matchIfMissing = true)
public class MockEmailProvider implements NotificacionProvider {

    public static final String CANAL = "EMAIL";
    private static final Logger log = LoggerFactory.getLogger(MockEmailProvider.class);

    @Override
    public String canal() {
        return CANAL;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        String mensajeId = "mock-email-" + UUID.randomUUID();
        log.info("[EMAIL-MOCK] id={} usuario={} asunto='{}' contenido='{}'",
                mensajeId,
                notificacion.getIdUsuario(),
                notificacion.getAsunto(),
                notificacion.getContenido());
        return ResultadoEnvio.ok(mensajeId);
    }
}
