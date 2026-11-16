package notificaciones.delivery;

import notificaciones.entity.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envío real de email por SMTP. En local apunta a MailHog (`localhost:1025`) y los mensajes se
 * ven en <a href="http://localhost:8025">http://localhost:8025</a>.
 *
 * <p>Se activa con {@code ticketya.notificaciones.email.provider=smtp}. Si el cliente no tiene
 * email resuelto se usa {@code ticketya.notificaciones.email.destino-default} para no perder el
 * aviso (y que MailHog siempre muestre algo).
 */
@Component
@ConditionalOnProperty(
        name = "ticketya.notificaciones.email.provider",
        havingValue = "smtp")
public class SmtpEmailProvider implements NotificacionProvider {

    public static final String CANAL = "EMAIL";
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final JavaMailSender mailSender;

    @Value("${ticketya.notificaciones.email.remitente:no-reply@ticketya.local}")
    private String remitente;

    @Value("${ticketya.notificaciones.email.destino-default:destinatarios@ticketya.local}")
    private String destinoDefault;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String canal() {
        return CANAL;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        String destino = notificacion.getDestinatario() == null
                        || notificacion.getDestinatario().isBlank()
                ? destinoDefault
                : notificacion.getDestinatario();

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject(notificacion.getAsunto());
        mensaje.setText(notificacion.getContenido());
        mailSender.send(mensaje);

        log.info("[EMAIL-SMTP] id={} destino='{}' asunto='{}'",
                notificacion.getIdNotificacion(), destino, notificacion.getAsunto());
        return ResultadoEnvio.ok("smtp:" + destino);
    }
}
