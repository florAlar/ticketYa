package eventos.clients;

import eventos.dto.CoordenadasDTO;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador Mock para geocodificación (activo por defecto: {@code ticketya.geocoding.provider=mock}).
 * Resuelve coordenadas realistas de ciudades y lugares conocidos de forma determinística
 * sin depender de llamadas externas de red. Reemplazable implementando {@link GeocodingProvider}.
 */
@Component
@ConditionalOnProperty(name = "ticketya.geocoding.provider", havingValue = "mock", matchIfMissing = true)
public class MockGeocodingAdapter implements GeocodingProvider {

    private static final Map<String, double[]> LUGARES_CONOCIDOS = new LinkedHashMap<>();
    private static final Map<String, double[]> CIUDADES_CONOCIDAS = new LinkedHashMap<>();

    static {
        LUGARES_CONOCIDOS.put("figueroa alcorta", new double[] {-34.545300, -58.449700});
        LUGARES_CONOCIDOS.put("monumental", new double[] {-34.545300, -58.449700});
        LUGARES_CONOCIDOS.put("ciudad del rock", new double[] {-34.677500, -58.449200});
        LUGARES_CONOCIDOS.put("escalada", new double[] {-34.677500, -58.449200});
        LUGARES_CONOCIDOS.put("interlagos", new double[] {-23.703600, -46.699700});
        LUGARES_CONOCIDOS.put("senador teotonio", new double[] {-23.703600, -46.699700});
        LUGARES_CONOCIDOS.put("movistar arena", new double[] {-34.594300, -58.447800});
        LUGARES_CONOCIDOS.put("humboldt", new double[] {-34.594300, -58.447800});
        LUGARES_CONOCIDOS.put("luna park", new double[] {-34.602000, -58.368800});
        LUGARES_CONOCIDOS.put("madero", new double[] {-34.602000, -58.368800});
        LUGARES_CONOCIDOS.put("velez", new double[] {-34.635400, -58.520700});
        LUGARES_CONOCIDOS.put("amalfitani", new double[] {-34.635400, -58.520700});
        LUGARES_CONOCIDOS.put("obras", new double[] {-34.545800, -58.458100});
        LUGARES_CONOCIDOS.put("libertador 7395", new double[] {-34.545800, -58.458100});
        LUGARES_CONOCIDOS.put("kempes", new double[] {-31.369000, -64.246200});
        LUGARES_CONOCIDOS.put("maradona", new double[] {-34.913800, -57.989000});

        CIUDADES_CONOCIDAS.put("buenos aires", new double[] {-34.603722, -58.381592});
        CIUDADES_CONOCIDAS.put("caba", new double[] {-34.603722, -58.381592});
        CIUDADES_CONOCIDAS.put("capital federal", new double[] {-34.603722, -58.381592});
        CIUDADES_CONOCIDAS.put("tandil", new double[] {-37.321670, -59.133160});
        CIUDADES_CONOCIDAS.put("la plata", new double[] {-34.920495, -57.953566});
        CIUDADES_CONOCIDAS.put("mar del plata", new double[] {-38.005477, -57.542611});
        CIUDADES_CONOCIDAS.put("cordoba", new double[] {-31.420083, -64.188776});
        CIUDADES_CONOCIDAS.put("rosario", new double[] {-32.944243, -60.650539});
        CIUDADES_CONOCIDAS.put("mendoza", new double[] {-32.889459, -68.845839});
        CIUDADES_CONOCIDAS.put("salta", new double[] {-24.782127, -65.423198});
        CIUDADES_CONOCIDAS.put("tucuman", new double[] {-26.808285, -65.217590});
        CIUDADES_CONOCIDAS.put("neuquen", new double[] {-38.951610, -68.059100});
        CIUDADES_CONOCIDAS.put("bariloche", new double[] {-41.133472, -71.310278});
        CIUDADES_CONOCIDAS.put("santa fe", new double[] {-31.610658, -60.697294});
        CIUDADES_CONOCIDAS.put("bahia blanca", new double[] {-38.718318, -62.266348});
        CIUDADES_CONOCIDAS.put("sao paulo", new double[] {-23.550520, -46.633308});
        CIUDADES_CONOCIDAS.put("rio de janeiro", new double[] {-22.906847, -43.172896});
        CIUDADES_CONOCIDAS.put("montevideo", new double[] {-34.901113, -56.164531});
        CIUDADES_CONOCIDAS.put("santiago", new double[] {-33.448890, -70.669265});
        CIUDADES_CONOCIDAS.put("madrid", new double[] {40.416775, -3.703790});
    }

    @Override
    public Optional<CoordenadasDTO> geocode(String direccion, String ciudad) {
        String ciuNorm = normalizar(ciudad);
        String dirNorm = normalizar(direccion);

        if (ciuNorm.isEmpty()
                || ciuNorm.contains("inexistente")
                || ciuNorm.contains("unknown")
                || ciuNorm.contains("[not_found]")
                || dirNorm.contains("inexistente")
                || dirNorm.contains("[not_found]")) {
            return Optional.empty();
        }
        if (dirNorm.contains("[fail]") || ciuNorm.contains("[fail]")) {
            throw new IllegalStateException("Fallo simulado del adaptador de geocodificación");
        }

        if (!dirNorm.isEmpty()) {
            for (Map.Entry<String, double[]> e : LUGARES_CONOCIDOS.entrySet()) {
                if (dirNorm.contains(e.getKey())) {
                    return Optional.of(toDto(e.getValue()[0], e.getValue()[1]));
                }
            }
        }

        double[] base = null;
        for (Map.Entry<String, double[]> e : CIUDADES_CONOCIDAS.entrySet()) {
            if (ciuNorm.contains(e.getKey())) {
                base = e.getValue();
                break;
            }
        }
        if (base == null) {
            return Optional.empty();
        }

        double deltaLat = 0.0;
        double deltaLng = 0.0;
        if (!dirNorm.isEmpty()) {
            int hash = Math.abs(dirNorm.hashCode());
            deltaLat = ((hash % 400) - 200) * 0.00005;
            deltaLng = (((hash / 400) % 400) - 200) * 0.00005;
        }
        return Optional.of(toDto(base[0] + deltaLat, base[1] + deltaLng));
    }

    @Override
    public String providerName() {
        return "mock";
    }

    private static CoordenadasDTO toDto(double lat, double lng) {
        return new CoordenadasDTO(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
    }

    private static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String sinTildes = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
