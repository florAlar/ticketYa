package e2e;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base de los escenarios E2E de TicketYa.
 *
 * <p>Habla <b>solo con el gateway</b> ({@code http://localhost:8080/api}); no levanta contextos
 * Spring ni usa Testcontainers. La infraestructura la administra {@code scripts/e2e.sh}.
 *
 * <p>Los helpers esperan con Awaitility porque varios efectos son asincrónicos: emisión de
 * entradas, ingesta del historial, notificaciones por RabbitMQ y envío de emails por el job.
 */
abstract class E2EBase {

    protected static final String BASE_URL =
            System.getProperty("e2e.baseUrl", "http://localhost:8080");
    protected static final String MAILHOG_URL =
            System.getProperty("e2e.mailhogUrl", "http://localhost:8025");

    protected static final String EMAIL_DEMO = "demo@ticketya.com";
    protected static final String EMAIL_ORG = "org@ticketya.com";
    protected static final String EMAIL_ADMIN = "admin@ticketya.com";
    protected static final String PASSWORD_DEMO = "demo1234";
    protected static final String PASSWORD_ORG = "org1234";
    protected static final String PASSWORD_ADMIN = "admin1234";

    protected static final String USER_DEMO = "b0000000-0000-4000-8000-000000000001";
    protected static final String USER_ORG = "b0000000-0000-4000-8000-000000000002";
    protected static final String USER_ADMIN = "b0000000-0000-4000-8000-000000000003";

    protected static final String EVENTO_SEED = "d0000000-0000-4000-8000-000000000001";
    protected static final String LUGAR_SEED = "c0000000-0000-4000-8000-000000000001";
    protected static final String SECTOR_SEED = "c0000000-0000-4000-8000-000000000011";
    protected static final String PARTICIPANTE_METALLICA = "p0000000-0000-4000-8000-000000000001";
    protected static final String DEVOLUCION_SEED = "aa000000-0000-4000-8000-000000000011";
    protected static final String ENTRADA_SEED = "aa000000-0000-4000-8000-000000000001";

    protected static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final Map<String, String> TOKENS = new HashMap<>();

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected record Sesion(String token, String userId, String email) {}

    protected record Compra(String id, String reservaId) {}

    @BeforeAll
    static void esperarStack() {
        await().atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    Sesion demo = demo();
                    assertEquals(200, publico().get("/api/eventos").statusCode(),
                            "eventos no responde");
                    assertEquals(200, publico().get("/api/eventos/{id}/disponibilidad", EVENTO_SEED)
                                    .statusCode(),
                            "ventas no responde");
                    assertEquals(200, api(demo.token()).get("/api/notificaciones/me").statusCode(),
                            "notificaciones no responde");
                    assertEquals(200, api(demo.token()).get("/api/entradas").statusCode(),
                            "entradas no responde");
                    assertEquals(200, api(demo.token()).get("/api/preferencias/me").statusCode(),
                            "preferencias no responde");
                    assertEquals(200, api(demo.token())
                                    .get("/api/clientes/{id}/historial", demo.userId()).statusCode(),
                            "historial no responde");
                });
    }

    // --- clientes HTTP ---

    protected static RequestSpecification publico() {
        return given().baseUri(BASE_URL).accept(ContentType.JSON);
    }

    protected static RequestSpecification api() {
        return given().baseUri(BASE_URL).accept(ContentType.JSON).contentType(ContentType.JSON);
    }

    protected static RequestSpecification api(String token) {
        return api().header("Authorization", "Bearer " + token);
    }

    // --- sesiones ---

    protected static String login(String email, String password) {
        return TOKENS.computeIfAbsent(email, key -> api()
                .body(Map.of("email", email, "password", password))
                .post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token"));
    }

    protected static Sesion demo() {
        return new Sesion(login(EMAIL_DEMO, PASSWORD_DEMO), USER_DEMO, EMAIL_DEMO);
    }

    protected static Sesion organizador() {
        return new Sesion(login(EMAIL_ORG, PASSWORD_ORG), USER_ORG, EMAIL_ORG);
    }

    protected static Sesion admin() {
        return new Sesion(login(EMAIL_ADMIN, PASSWORD_ADMIN), USER_ADMIN, EMAIL_ADMIN);
    }

    /** Alta de un cliente nuevo para aislar cada escenario (sin límites ni bandeja compartida). */
    protected static Sesion registrarCliente() {
        String email = "e2e-" + UUID.randomUUID().toString().substring(0, 8) + "@ticketya.com";
        JsonPath sesion = api()
                .body(Map.of(
                        "nombre", "E2E",
                        "apellido", "Test",
                        "email", email,
                        "password", "secreto123",
                        "ciudad", "Buenos Aires"))
                .post("/api/auth/register")
                .then().statusCode(200)
                .extract().jsonPath();
        return new Sesion(sesion.getString("token"), sesion.getString("user.id"), email);
    }

    // --- flujo de compra ---

    /**
     * Crea un evento en el lugar seed (Buenos Aires) y lo publica, para que ventas abra cupos.
     * Si {@code participanteId} no es null, lo asocia antes de publicar.
     */
    protected static String crearEventoPublicado(
            Sesion organizador, String nombre, String participanteId, int cupo) {
        LocalDateTime inicio = LocalDateTime.now().plusMonths(6).withNano(0);
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("descripcion", "Escenario E2E " + nombre);
        body.put("tipo", "RECITAL");
        body.put("fechaInicio", inicio.format(FECHA));
        body.put("fechaFin", inicio.plusHours(3).format(FECHA));
        body.put("estado", "BORRADOR");
        body.put("idLugar", LUGAR_SEED);
        body.put("sectores", List.of(Map.of(
                "id", SECTOR_SEED,
                "precio", 50000,
                "capacidadTotal", cupo)));

        String eventoId = api(organizador.token())
                .body(body)
                .post("/api/eventos")
                .then().statusCode(201)
                .extract().path("id");

        if (participanteId != null) {
            api(organizador.token())
                    .body(Map.of("ids", List.of(participanteId)))
                    .put("/api/eventos/{id}/participantes", eventoId)
                    .then().statusCode(200);
        }

        api(organizador.token())
                .post("/api/eventos/{id}/publicacion", eventoId)
                .then().statusCode(200);
        // ventas abre el stock comercial al consumir evento.publicado (asincrónico):
        // sin esta espera la primera reserva puede llegar antes que el cupo.
        esperarDisponible(eventoId, SECTOR_SEED, cupo);
        return eventoId;
    }

    /** Reserva → confirma → compra → paga APROBAR. Devuelve los ids de compra y reserva. */
    protected static Compra comprar(Sesion cliente, String eventoId, int cantidad) {
        String reservaId = api(cliente.token())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(Map.of(
                        "idEvento", eventoId,
                        "items", List.of(Map.of("idSector", SECTOR_SEED, "cantidad", cantidad))))
                .post("/api/reservas")
                .then().statusCode(201)
                .extract().path("id");

        api(cliente.token())
                .post("/api/reservas/{id}/confirmacion", reservaId)
                .then().statusCode(200);

        String compraId = api(cliente.token())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(Map.of("idReserva", reservaId))
                .post("/api/compras")
                .then().statusCode(201)
                .extract().path("id");

        api(cliente.token())
                .body(Map.of("resultado", "APROBAR"))
                .post("/api/compras/{id}/pagos", compraId)
                .then().statusCode(201);

        return new Compra(compraId, reservaId);
    }

    // --- esperas de efectos asincrónicos ---

    protected static List<Map<String, Object>> esperarEntradas(
            Sesion cliente, String compraId, int cantidad) {
        AtomicReference<List<Map<String, Object>>> resultado = new AtomicReference<>(List.of());
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<Map<String, Object>> todas = api(cliente.token())
                            .get("/api/entradas")
                            .then().statusCode(200)
                            .extract().jsonPath()
                            .getList("$");
                    List<Map<String, Object>> propias = todas.stream()
                            .filter(e -> compraId.equals(e.get("idCompra")))
                            .toList();
                    assertEquals(cantidad, propias.size(),
                            "entradas emitidas para la compra " + compraId);
                    assertTrue(propias.stream().allMatch(e -> "VENDIDA".equals(e.get("estado"))),
                            "todas las entradas deben quedar VENDIDA");
                    resultado.set(propias);
                });
        return resultado.get();
    }

    protected static void esperarHistorial(Sesion cliente, String eventoId) {
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<Map<String, Object>> items = api(cliente.token())
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .get("/api/clientes/{id}/historial", cliente.userId())
                            .then().statusCode(200)
                            .extract().jsonPath()
                            .getList("content");
                    assertTrue(items.stream().anyMatch(i -> eventoId.equals(i.get("idEvento"))
                                    && "COMPRA".equals(i.get("tipo"))),
                            "el historial debe incluir la compra del evento " + eventoId);
                });
    }

    protected static Map<String, Object> esperarNotificacion(
            Sesion cliente, String tipo, String eventoId) {
        AtomicReference<Map<String, Object>> encontrada = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<Map<String, Object>> content = notificaciones(cliente);
                    Map<String, Object> match = content.stream()
                            .filter(n -> tipo.equals(n.get("tipo")))
                            .filter(n -> eventoId == null || eventoId.equals(n.get("idEventoOrigen")))
                            .findFirst()
                            .orElse(null);
                    assertNotNull(match, "notificación " + tipo + " del evento " + eventoId);
                    encontrada.set(match);
                });
        return encontrada.get();
    }

    protected static List<Map<String, Object>> notificaciones(Sesion cliente) {
        return api(cliente.token())
                .queryParam("page", 0)
                .queryParam("size", 100)
                .get("/api/notificaciones/me")
                .then().statusCode(200)
                .extract().jsonPath()
                .getList("content");
    }

    @SuppressWarnings("unchecked")
    protected static List<String> canales(Map<String, Object> notificacion) {
        return (List<String>) notificacion.get("canales");
    }

    protected static void assertCanalesPushYEmail(Map<String, Object> notificacion) {
        List<String> canales = canales(notificacion);
        assertNotNull(canales, "la notificación debe informar canales");
        assertTrue(canales.contains("PUSH"), "falta PUSH en " + canales);
        assertTrue(canales.contains("EMAIL"), "falta EMAIL en " + canales);
    }

    protected static void esperarEmail(String destinatario, String asuntoContiene) {
        await().atMost(Duration.ofSeconds(150))
                .pollInterval(Duration.ofSeconds(3))
                .untilAsserted(() -> assertTrue(
                        mailHogContiene(destinatario, asuntoContiene),
                        "email a " + destinatario + " con asunto '" + asuntoContiene + "'"));
    }

    @SuppressWarnings("unchecked")
    private static boolean mailHogContiene(String destinatario, String asuntoContiene) {
        JsonPath mensajes = given().baseUri(MAILHOG_URL).accept(ContentType.JSON)
                .get("/api/v2/messages")
                .then().statusCode(200)
                .extract().jsonPath();
        List<Map<String, Object>> items = mensajes.getList("items");
        if (items == null) {
            return false;
        }
        return items.stream().anyMatch(item -> {
            Map<String, Object> content = (Map<String, Object>) item.get("Content");
            if (content == null) {
                return false;
            }
            Map<String, Object> headers = (Map<String, Object>) content.get("Headers");
            if (headers == null) {
                return false;
            }
            List<String> to = (List<String>) headers.getOrDefault("To", List.of());
            List<String> subject = (List<String>) headers.getOrDefault("Subject", List.of());
            return to.stream().anyMatch(t -> t.contains(destinatario))
                    && subject.stream()
                            .map(E2EBase::decodificarAsunto)
                            .anyMatch(s -> s.contains(asuntoContiene));
        });
    }

    /**
     * MailHog entrega el header {@code Subject} tal cual lo escribe JavaMail: los acentos viajan
     * como palabras codificadas MIME ({@code =?UTF-8?Q?...?=}). Se decodifican para poder comparar
     * asuntos en español.
     */
    static String decodificarAsunto(String asunto) {
        if (asunto == null || !asunto.contains("=?")) {
            return asunto;
        }
        Matcher matcher = Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=").matcher(asunto);
        StringBuilder texto = new StringBuilder();
        int ultimo = 0;
        while (matcher.find()) {
            texto.append(asunto, ultimo, matcher.start());
            try {
                if ("B".equalsIgnoreCase(matcher.group(2))) {
                    texto.append(new String(
                            Base64.getDecoder().decode(matcher.group(3)), StandardCharsets.UTF_8));
                } else {
                    texto.append(decodificarQuotedPrintable(matcher.group(3)));
                }
            } catch (RuntimeException ex) {
                texto.append(matcher.group(0));
            }
            ultimo = matcher.end();
        }
        return texto.append(asunto.substring(ultimo)).toString();
    }

    private static String decodificarQuotedPrintable(String valor) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '_') {
                bytes.write(' ');
            } else if (c == '=' && i + 2 < valor.length()) {
                bytes.write(Integer.parseInt(valor.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                bytes.write(c);
            }
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    // --- disponibilidad ---

    protected static JsonPath disponibilidad(String eventoId) {
        return publico()
                .get("/api/eventos/{id}/disponibilidad", eventoId)
                .then().statusCode(200)
                .extract().jsonPath();
    }

    protected static int disponibleSector(JsonPath disponibilidad, String sectorId) {
        List<Map<String, Object>> sectores = disponibilidad.getList("sectores");
        return sectores.stream()
                .filter(s -> sectorId.equals(s.get("idSector")))
                .mapToInt(s -> ((Number) s.get("cantidadDisponible")).intValue())
                .findFirst()
                .orElseThrow(() -> new AssertionError("sector " + sectorId + " no informado"));
    }

    protected static void esperarDisponible(String eventoId, String sectorId, int esperado) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(esperado,
                        disponibleSector(disponibilidad(eventoId), sectorId)));
    }

    protected static void esperarDisponibleAlMenos(String eventoId, String sectorId, int minimo) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> assertTrue(
                        disponibleSector(disponibilidad(eventoId), sectorId) >= minimo,
                        "disponibilidad del sector " + sectorId + " >= " + minimo));
    }

    // --- utilidades ---

    protected static BigDecimal decimal(JsonPath json, String path) {
        return new BigDecimal(json.getString(path));
    }

    protected static void assertEqualsImporte(BigDecimal esperado, BigDecimal real) {
        assertEquals(0, esperado.compareTo(real),
                "importe esperado " + esperado + " pero fue " + real);
    }
}
