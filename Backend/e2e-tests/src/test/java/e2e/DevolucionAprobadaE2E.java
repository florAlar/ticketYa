package e2e;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * F2 — Devolución aprobada: entradas → ventas → notificaciones.
 *
 * <p>Aprueba la devolución seed pendiente y verifica el reintegro de la entrada, la liberación
 * de cupo, la notificación dual PUSH/EMAIL y el correo en MailHog.
 */
class DevolucionAprobadaE2E extends E2EBase {

    @Test
    void aprobarDevolucionLiberaCupoYNotificaPorPushYEmail() {
        Sesion admin = admin();
        Sesion demo = demo();

        Map<String, Object> devolucion = api(admin.token())
                .get("/api/devoluciones/{id}", DEVOLUCION_SEED)
                .then().statusCode(200)
                .extract().jsonPath().getMap("$");
        assumeTrue("PENDIENTE".equals(devolucion.get("estado")),
                "La devolución seed ya fue procesada; correr scripts/e2e.sh para resetear volúmenes");

        int disponibleAntes = disponibleSector(disponibilidad(EVENTO_SEED), SECTOR_SEED);

        api(admin.token())
                .post("/api/devoluciones/{id}/aprobacion", DEVOLUCION_SEED)
                .then().statusCode(200);

        await().atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    JsonPath entrada = api(demo.token())
                            .get("/api/entradas/{id}", ENTRADA_SEED)
                            .then().statusCode(200)
                            .extract().jsonPath();
                    assertEquals("DEVUELTA", entrada.getString("estado"));
                    assertNull(entrada.getString("codigoQR"),
                            "una entrada devuelta no debe exhibir QR");
                });

        esperarDisponibleAlMenos(EVENTO_SEED, SECTOR_SEED, disponibleAntes + 1);

        Map<String, Object> notificacion =
                esperarNotificacion(demo, "DevolucionAprobada", EVENTO_SEED);
        assertCanalesPushYEmail(notificacion);
        assertTrue(String.valueOf(notificacion.get("contenido")).contains("95000"));

        long mensajes = notificaciones(demo).stream()
                .filter(n -> "DevolucionAprobada".equals(n.get("tipo")))
                .filter(n -> EVENTO_SEED.equals(n.get("idEventoOrigen")))
                .count();
        assertEquals(1, mensajes, "PUSH y EMAIL del mismo aviso se agrupan en una entrada");

        esperarEmail(demo.email(), "Devolución aprobada");
    }
}
