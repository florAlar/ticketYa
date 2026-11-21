package e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.path.json.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * F5 — Reporte del organizador: ventas → entradas + eventos.
 *
 * <p>Crea un evento con una compra aprobada y valida que los totales, la ocupación y la
 * recaudación neta cuadren entre el reporte completo y los sub-reportes.
 */
class ReporteOrganizadorE2E extends E2EBase {

    private static final BigDecimal PRECIO = new BigDecimal("50000.00");

    @Test
    void reporteDelEventoCuadraConLasVentas() {
        Sesion org = organizador();
        String eventoId = crearEventoPublicado(
                org, "E2E Reporte " + System.currentTimeMillis(), null, 10);
        Sesion cliente = registrarCliente();
        Compra compra = comprar(cliente, eventoId, 2);
        esperarEntradas(cliente, compra.id(), 2);

        JsonPath reporte = api(org.token())
                .get("/api/reportes/eventos/{id}", eventoId)
                .then().statusCode(200)
                .extract().jsonPath();

        assertEquals("ARS", reporte.getString("moneda"));
        assertEquals("TOTAL", reporte.getString("total.nombre"));
        assertEquals(2, reporte.getLong("total.vendidas"));
        assertEquals(2, reporte.getLong("total.vigentes"));
        assertEquals(0, reporte.getLong("total.devueltas"));
        assertEquals(0, reporte.getLong("total.aReintegrar"));
        assertEquals(0, reporte.getLong("total.ingresaron"));
        assertEquals(10, reporte.getLong("total.capacidad"));

        List<Map<String, Object>> sectores = reporte.getList("sectores");
        assertEquals(1, sectores.size());
        assertTrue(sectores.get(0).get("nombre") != null, "el sector debe tener nombre");

        BigDecimal bruta = decimal(reporte, "total.recaudacionBruta");
        BigDecimal devuelta = decimal(reporte, "total.recaudacionDevuelta");
        BigDecimal aReintegrar = decimal(reporte, "total.recaudacionAReintegrar");
        BigDecimal neta = decimal(reporte, "total.recaudacionNeta");
        assertEqualsImporte(PRECIO.multiply(new BigDecimal("2")), bruta);
        assertEqualsImporte(bruta.subtract(devuelta).subtract(aReintegrar), neta);

        double ocupacion = reporte.getDouble("total.ocupacionPct");
        assertTrue(ocupacion > 0 && ocupacion <= 100,
                "ocupación fuera de rango: " + ocupacion);

        assertEquals(reporte.getLong("total.vendidas"), sumar(sectores, "vendidas"));
        assertEquals(reporte.getLong("total.vigentes"), sumar(sectores, "vigentes"));
        assertEquals(reporte.getLong("total.ingresaron"), sumar(sectores, "ingresaron"));
        assertEquals(reporte.getLong("total.capacidad"), sumar(sectores, "capacidad"));

        JsonPath ventas = api(org.token())
                .get("/api/reportes/eventos/{id}/ventas", eventoId)
                .then().statusCode(200)
                .extract().jsonPath();
        assertEquals(1, ventas.getLong("compras"));
        assertEquals(2, ventas.getLong("entradasVendidas"));
        assertEquals(2, ventas.getLong("entradasVigentes"));

        JsonPath ocupacionReporte = api(org.token())
                .get("/api/reportes/eventos/{id}/ocupacion", eventoId)
                .then().statusCode(200)
                .extract().jsonPath();
        assertEquals(10, ocupacionReporte.getInt("capacidadTotal"));
        assertEquals(2, ocupacionReporte.getLong("vigentes"));

        JsonPath recaudacion = api(org.token())
                .get("/api/reportes/eventos/{id}/recaudacion", eventoId)
                .then().statusCode(200)
                .extract().jsonPath();
        assertEquals("ARS", recaudacion.getString("moneda"));
        assertEqualsImporte(bruta, decimal(recaudacion, "bruta"));
        assertEqualsImporte(neta, decimal(recaudacion, "neta"));

        JsonPath porSector = api(org.token())
                .get("/api/reportes/eventos/{id}/sectores", eventoId)
                .then().statusCode(200)
                .extract().jsonPath();
        assertEquals(1, porSector.getList("$").size());
    }

    private static long sumar(List<Map<String, Object>> sectores, String campo) {
        return sectores.stream()
                .mapToLong(s -> ((Number) s.get(campo)).longValue())
                .sum();
    }
}
