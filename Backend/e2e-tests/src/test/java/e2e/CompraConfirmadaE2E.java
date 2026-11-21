package e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * F1 — Compra confirmada: ventas → entradas → notificaciones → preferencias.
 *
 * <p>Crea un evento propio, compra 2 entradas y verifica la emisión, el historial, la
 * notificación EMAIL, el correo en MailHog y el correlation id del gateway.
 */
class CompraConfirmadaE2E extends E2EBase {

    @Test
    void compraAprobadaEmiteEntradasNotificaYRegistraHistorial() {
        Sesion org = organizador();
        String eventoId = crearEventoPublicado(
                org, "E2E Compra " + System.currentTimeMillis(), PARTICIPANTE_METALLICA, 20);
        Sesion cliente = registrarCliente();

        Compra compra = comprar(cliente, eventoId, 2);

        Map<String, Object> detalle = api(cliente.token())
                .get("/api/compras/{id}", compra.id())
                .then().statusCode(200)
                .extract().jsonPath().getMap("$");
        assertEquals("APROBADA", detalle.get("estado"));

        List<Map<String, Object>> entradas = esperarEntradas(cliente, compra.id(), 2);
        assertEquals(2, entradas.stream().map(e -> e.get("id")).distinct().count(),
                "debe haber una entrada por unidad");
        esperarHistorial(cliente, eventoId);

        Map<String, Object> notificacion =
                esperarNotificacion(cliente, "CompraConfirmada", eventoId);
        assertEquals("EMAIL", notificacion.get("canal"));
        assertEquals(cliente.email(), notificacion.get("destinatario"));
        assertTrue(canales(notificacion).contains("EMAIL"));
        assertTrue(String.valueOf(notificacion.get("contenido")).contains(compra.id()));

        esperarEmail(cliente.email(), "Tu compra en TicketYa");
        esperarDisponible(eventoId, SECTOR_SEED, 18);

        int noLeidas = api(cliente.token())
                .queryParam("page", 0)
                .queryParam("size", 100)
                .get("/api/notificaciones/me")
                .then().statusCode(200)
                .extract().jsonPath().getInt("noLeidas");
        assertTrue(noLeidas >= 1, "noLeidas debe reflejar la notificación nueva");

        Response respuesta = api(cliente.token())
                .get("/api/compras/{id}", compra.id())
                .then().statusCode(200)
                .extract().response();
        assertNotNull(respuesta.header("X-Correlation-Id"),
                "el gateway debe devolver X-Correlation-Id");
    }
}
