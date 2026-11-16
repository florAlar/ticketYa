package eventos.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eventos.dto.CoordenadasDTO;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GeocodingClientTest {

    @Test
    void mockAdapter_resuelveLugarYCiudadConocida_yDescartaInexistente() {
        MockGeocodingAdapter adapter = new MockGeocodingAdapter();

        Optional<CoordenadasDTO> monumental =
                adapter.geocode("Av. Figueroa Alcorta 7597", "Buenos Aires");
        assertTrue(monumental.isPresent());
        assertEquals(0, new BigDecimal("-34.545300").compareTo(monumental.get().latitud()));
        assertEquals(0, new BigDecimal("-58.449700").compareTo(monumental.get().longitud()));

        Optional<CoordenadasDTO> tandil = adapter.geocode("Pinto 399", "Tandil");
        assertTrue(tandil.isPresent());

        assertTrue(adapter.geocode("Calle Falsa 123", "Ciudad Inexistente").isEmpty());
    }

    @Test
    void cacheHit_noVuelveAConsultarAlProveedor() {
        AtomicInteger llamadas = new AtomicInteger(0);
        GeocodingProvider provider = (dir, ciu) -> {
            llamadas.incrementAndGet();
            return Optional.of(new CoordenadasDTO(new BigDecimal("-37.321670"), new BigDecimal("-59.133160")));
        };
        GeocodingClient client = new GeocodingClient(provider, true, 1000L, 2, 60L);

        Optional<CoordenadasDTO> r1 = client.resolver("Pinto 399", "Tandil");
        Optional<CoordenadasDTO> r2 = client.resolver("  pinto 399 ", "TANDIL ");

        assertTrue(r1.isPresent());
        assertTrue(r2.isPresent());
        assertEquals(1, llamadas.get(), "La segunda consulta debe resolverse desde caché");
        assertEquals(1, client.tamanoCache());
    }

    @Test
    void reintentos_siFallaUnaVezYRecuperaEnLaSegunda_devuelveCoordenadas() {
        AtomicInteger intentos = new AtomicInteger(0);
        GeocodingProvider provider = (dir, ciu) -> {
            if (intentos.incrementAndGet() == 1) {
                throw new RuntimeException("Error transitorio");
            }
            return Optional.of(new CoordenadasDTO(new BigDecimal("-34.603722"), new BigDecimal("-58.381592")));
        };
        GeocodingClient client = new GeocodingClient(provider, true, 1000L, 2, 60L);

        Optional<CoordenadasDTO> res = client.resolver("Av. Corrientes 123", "Buenos Aires");

        assertTrue(res.isPresent());
        assertEquals(2, intentos.get());
    }

    @Test
    void fallback_siFallaSiempre_devuelveVacioYNoCacheaFallo() {
        AtomicInteger intentos = new AtomicInteger(0);
        GeocodingProvider provider = (dir, ciu) -> {
            intentos.incrementAndGet();
            throw new RuntimeException("Proveedor caído");
        };
        GeocodingClient client = new GeocodingClient(provider, true, 1000L, 2, 60L);

        Optional<CoordenadasDTO> res = client.resolver("Av. Corrientes 123", "Buenos Aires");

        assertTrue(res.isEmpty());
        assertEquals(3, intentos.get());
        assertEquals(0, client.tamanoCache());
    }

    @Test
    void timeout_cortaPorTiempoYHaceFallbackAVacio() {
        AtomicInteger intentos = new AtomicInteger(0);
        GeocodingProvider slowProvider = (dir, ciu) -> {
            intentos.incrementAndGet();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(new CoordenadasDTO(new BigDecimal("-34.603722"), new BigDecimal("-58.381592")));
        };
        GeocodingClient client = new GeocodingClient(slowProvider, true, 40L, 1, 60L);

        Optional<CoordenadasDTO> res = client.resolver("Av. Corrientes 123", "Buenos Aires");

        assertTrue(res.isEmpty());
        assertEquals(2, intentos.get());
    }
}
