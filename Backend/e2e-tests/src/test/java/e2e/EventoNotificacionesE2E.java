package e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * F3 — Cambios de evento: eventos → notificaciones.
 *
 * <p>Un cliente con compra aprobada recibe tres avisos distintos al reprogramar, suspender y
 * cancelar el evento. Cada aviso se agrupa en una sola entrada con canales PUSH y EMAIL.
 */
class EventoNotificacionesE2E extends E2EBase {

    @Test
    void modificarSuspenderYCancelarNotificanAlComprador() {
        Sesion org = organizador();
        Sesion admin = admin();
        String eventoId = crearEventoPublicado(
                org, "E2E Evento " + System.currentTimeMillis(), null, 10);
        Sesion cliente = registrarCliente();
        Compra compra = comprar(cliente, eventoId, 1);
        esperarEntradas(cliente, compra.id(), 1);

        LocalDateTime nuevaFecha = LocalDateTime.now().plusMonths(7).withNano(0);
        api(org.token())
                .body(Map.of(
                        "fechaInicio", nuevaFecha.format(FECHA),
                        "fechaFin", nuevaFecha.plusHours(3).format(FECHA)))
                .patch("/api/eventos/{id}/fecha", eventoId)
                .then().statusCode(200);
        verificarAviso(cliente, eventoId, "EventoModificado");

        api(admin.token())
                .body(Map.of("estado", "SUSPENDIDO"))
                .patch("/api/eventos/{id}/estado", eventoId)
                .then().statusCode(200);
        verificarAviso(cliente, eventoId, "EventoSuspendido");

        api(admin.token())
                .body(Map.of("estado", "CANCELADO"))
                .patch("/api/eventos/{id}/estado", eventoId)
                .then().statusCode(200);
        verificarAviso(cliente, eventoId, "EventoCancelado");

        esperarEmail(cliente.email(), "Evento cancelado");
    }

    private void verificarAviso(Sesion cliente, String eventoId, String tipo) {
        Map<String, Object> notificacion = esperarNotificacion(cliente, tipo, eventoId);
        assertCanalesPushYEmail(notificacion);

        long mensajes = notificaciones(cliente).stream()
                .filter(n -> tipo.equals(n.get("tipo")))
                .filter(n -> eventoId.equals(n.get("idEventoOrigen")))
                .count();
        assertEquals(1, mensajes, tipo + " debe aparecer una sola vez en la bandeja");
    }
}
