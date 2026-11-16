package eventos.clients;

import eventos.dto.CoordenadasDTO;
import java.util.Optional;

/**
 * Puerto del patrón Adaptador para proveedores de geocodificación (LUG-03).
 * Permite intercambiar el proveedor (mock, Nominatim/OSM, Google Maps, Mapbox)
 * sin acoplar el dominio ni la capa de resiliencia y caché.
 */
public interface GeocodingProvider {

    /**
     * Resuelve coordenadas geográficas a partir de dirección y ciudad.
     *
     * @param direccion calle y altura (puede ser null o vacía si se busca solo por ciudad)
     * @param ciudad ciudad del lugar (obligatoria)
     * @return coordenadas encontradas o {@link Optional#empty()} si no hubo coincidencias
     */
    Optional<CoordenadasDTO> geocode(String direccion, String ciudad);

    /** Nombre identificador del proveedor activo (ej. "mock", "nominatim"). */
    default String providerName() {
        return getClass().getSimpleName();
    }
}
