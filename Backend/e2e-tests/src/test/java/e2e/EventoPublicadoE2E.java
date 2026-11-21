package e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * F4 — Evento publicado + matching: eventos → preferencias → notificaciones.
 *
 * <p>Publica un evento en Buenos Aires con Metallica y verifica que el cliente demo, que sigue a
 * la banda y vive en esa ciudad, reciba el aviso de artista favorito por PUSH y EMAIL.
 */
class EventoPublicadoE2E extends E2EBase {

    @Test
    void publicarEventoConArtistaFavoritoNotificaAlClienteDemo() {
        Sesion org = organizador();
        String nombre = "E2E Metallica " + System.currentTimeMillis();
        String eventoId = crearEventoPublicado(org, nombre, PARTICIPANTE_METALLICA, 10);

        Sesion demo = demo();
        Map<String, Object> notificacion =
                esperarNotificacion(demo, "NuevoEventoArtistaFavorito", eventoId);
        assertCanalesPushYEmail(notificacion);

        String contenido = String.valueOf(notificacion.get("contenido"));
        assertTrue(contenido.contains(nombre), "el aviso debe nombrar el evento: " + contenido);
        assertTrue(contenido.contains("Buenos Aires"),
                "el aviso debe mencionar la ciudad: " + contenido);

        esperarDisponible(eventoId, SECTOR_SEED, 10);
        assertTrue(disponibilidad(eventoId).getInt("disponibleTotal") > 0,
                "publicar debe abrir cupos en ventas");
        esperarEmail(demo.email(), "Tu artista favorito se presenta");
    }
}
