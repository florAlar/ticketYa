package notificaciones.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import notificaciones.entity.Notificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmtpEmailProviderTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SmtpEmailProvider(mailSender);
        ReflectionTestUtils.setField(provider, "remitente", "no-reply@ticketya.local");
        ReflectionTestUtils.setField(provider, "destinoDefault", "destinatarios@ticketya.local");
    }

    @Test
    void enviaAlEmailResueltoDelCliente() {
        Notificacion n = notificacion("demo@ticketya.com");

        NotificacionProvider.ResultadoEnvio resultado = provider.enviar(n);

        assertTrue(resultado.exito());
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("no-reply@ticketya.local", msg.getFrom());
        assertEquals("demo@ticketya.com", msg.getTo()[0]);
        assertEquals("Tu compra en TicketYa", msg.getSubject());
        assertEquals("Compra confirmada", msg.getText());
    }

    @Test
    void sinDestinatarioUsaElDefaultDeMailHog() {
        Notificacion n = notificacion(null);

        provider.enviar(n);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("destinatarios@ticketya.local", captor.getValue().getTo()[0]);
    }

    private Notificacion notificacion(String destinatario) {
        Notificacion n = new Notificacion();
        n.setIdNotificacion("noti-1");
        n.setIdUsuario("cli-1");
        n.setCanal("EMAIL");
        n.setAsunto("Tu compra en TicketYa");
        n.setContenido("Compra confirmada");
        n.setDestinatario(destinatario);
        return n;
    }
}
