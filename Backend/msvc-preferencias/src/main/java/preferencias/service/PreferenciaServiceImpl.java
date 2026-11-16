package preferencias.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import preferencias.document.NotificacionesPreferencias;
import preferencias.document.PreferenciasCliente;
import preferencias.dto.MatchingClientesRequestDTO;
import preferencias.dto.NotificacionesPreferenciasDTO;
import preferencias.dto.PreferenciaRequestDTO;
import preferencias.dto.PreferenciaResponseDTO;
import preferencias.observability.Bitacora;
import preferencias.repository.PreferenciasClienteRepository;

@Service
public class PreferenciaServiceImpl implements PreferenciaService {

    private static final Logger log = LoggerFactory.getLogger(PreferenciaServiceImpl.class);

    private final PreferenciasClienteRepository repository;
    private final MongoTemplate mongoTemplate;

    public PreferenciaServiceImpl(
            PreferenciasClienteRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public PreferenciaResponseDTO obtener(String idCliente) {
        return repository.findByIdCliente(idCliente)
                .map(this::toResponse)
                .orElseGet(() -> vacia(idCliente));
    }

    @Override
    public PreferenciaResponseDTO actualizar(String idCliente, PreferenciaRequestDTO request) {
        PreferenciasCliente doc = repository.findByIdCliente(idCliente)
                .orElseGet(() -> nuevo(idCliente));
        if (request != null) {
            if (request.getCategorias() != null) {
                doc.setCategorias(limpiar(request.getCategorias()));
            }
            if (request.getCiudades() != null) {
                doc.setCiudades(limpiar(request.getCiudades()));
            }
            if (request.getRangoPrecio() != null) {
                doc.setRangoPrecio(request.getRangoPrecio());
            }
            if (request.getArtistas() != null) {
                doc.setArtistas(limpiar(request.getArtistas()));
            }
            if (request.getNotificaciones() != null) {
                NotificacionesPreferencias actuales = doc.getNotificaciones() == null
                        ? new NotificacionesPreferencias()
                        : doc.getNotificaciones();
                NotificacionesPreferenciasDTO dto = request.getNotificaciones();
                if (dto.getPush() != null) {
                    actuales.setPush(dto.getPush());
                }
                if (dto.getEmail() != null) {
                    actuales.setEmail(dto.getEmail());
                }
                if (dto.getTipos() != null) {
                    actuales.setTipos(limpiar(dto.getTipos()));
                }
                doc.setNotificaciones(actuales);
            }
        }
        doc.setFechaActualizacion(Instant.now());
        PreferenciasCliente guardado = repository.save(doc);
        Bitacora.registrar("PREFERENCIAS_ACTUALIZADAS", idCliente, idCliente, "artistas="
                + guardado.getArtistas().size() + " ciudades=" + guardado.getCiudades().size());
        return toResponse(guardado);
    }

    @Override
    public NotificacionesPreferenciasDTO obtenerNotificaciones(String idCliente) {
        return repository.findByIdCliente(idCliente)
                .map(PreferenciasCliente::getNotificaciones)
                .map(this::toDto)
                .orElseGet(() -> toDto(new NotificacionesPreferencias()));
    }

    @Override
    public NotificacionesPreferenciasDTO actualizarNotificaciones(
            String idCliente, NotificacionesPreferenciasDTO request) {
        PreferenciasCliente doc = repository.findByIdCliente(idCliente)
                .orElseGet(() -> nuevo(idCliente));
        NotificacionesPreferencias actuales =
                doc.getNotificaciones() == null ? new NotificacionesPreferencias() : doc.getNotificaciones();
        if (request != null) {
            if (request.getPush() != null) {
                actuales.setPush(request.getPush());
            }
            if (request.getEmail() != null) {
                actuales.setEmail(request.getEmail());
            }
            if (request.getTipos() != null) {
                actuales.setTipos(limpiar(request.getTipos()));
            }
        }
        doc.setNotificaciones(actuales);
        doc.setFechaActualizacion(Instant.now());
        repository.save(doc);
        Bitacora.registrar(
                "PREFERENCIAS_NOTIFICACION_ACTUALIZADAS", idCliente, idCliente,
                "push=" + actuales.getPush() + " email=" + actuales.getEmail());
        return toDto(actuales);
    }

    @Override
    public List<String> clientesInteresados(MatchingClientesRequestDTO request) {
        if (request == null
                || request.getCiudad() == null
                || request.getCiudad().isBlank()
                || request.getArtistas() == null
                || request.getArtistas().isEmpty()) {
            return List.of();
        }
        List<String> artistas = request.getArtistas().stream()
                .filter(a -> a != null && !a.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (artistas.isEmpty()) {
            return List.of();
        }

        Criteria ciudad = Criteria.where("ciudades")
                .regex("^" + Pattern.quote(request.getCiudad().trim()) + "$", "i");
        Criteria[] artistasCriteria = artistas.stream()
                .map(a -> Criteria.where("artistas").regex("^" + Pattern.quote(a) + "$", "i"))
                .toArray(Criteria[]::new);
        Criteria criterio = new Criteria()
                .andOperator(ciudad, new Criteria().orOperator(artistasCriteria));

        Query query = new Query(criterio);
        query.fields().include("idCliente");
        List<String> clientes = mongoTemplate.find(query, PreferenciasCliente.class).stream()
                .map(PreferenciasCliente::getIdCliente)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.info("Matching artista/ciudad ciudad={} artistas={} -> {} cliente(s)",
                request.getCiudad(), artistas, clientes.size());
        return clientes;
    }

    private PreferenciasCliente nuevo(String idCliente) {
        PreferenciasCliente doc = new PreferenciasCliente();
        doc.setIdCliente(idCliente);
        doc.setCategorias(new ArrayList<>());
        doc.setCiudades(new ArrayList<>());
        doc.setArtistas(new ArrayList<>());
        doc.setNotificaciones(new NotificacionesPreferencias());
        return doc;
    }

    private PreferenciaResponseDTO vacia(String idCliente) {
        PreferenciaResponseDTO dto = new PreferenciaResponseDTO();
        dto.setIdCliente(idCliente);
        dto.setCategorias(new ArrayList<>());
        dto.setCiudades(new ArrayList<>());
        dto.setArtistas(new ArrayList<>());
        dto.setNotificaciones(toDto(new NotificacionesPreferencias()));
        return dto;
    }

    private PreferenciaResponseDTO toResponse(PreferenciasCliente doc) {
        PreferenciaResponseDTO dto = new PreferenciaResponseDTO();
        dto.setIdCliente(doc.getIdCliente());
        dto.setCategorias(doc.getCategorias() == null ? List.of() : doc.getCategorias());
        dto.setCiudades(doc.getCiudades() == null ? List.of() : doc.getCiudades());
        dto.setRangoPrecio(doc.getRangoPrecio());
        dto.setArtistas(doc.getArtistas() == null ? List.of() : doc.getArtistas());
        dto.setNotificaciones(toDto(doc.getNotificaciones()));
        dto.setFechaActualizacion(doc.getFechaActualizacion());
        return dto;
    }

    private NotificacionesPreferenciasDTO toDto(NotificacionesPreferencias doc) {
        NotificacionesPreferenciasDTO dto = new NotificacionesPreferenciasDTO();
        if (doc != null) {
            dto.setPush(doc.getPush() == null || doc.getPush());
            dto.setEmail(doc.getEmail() == null || doc.getEmail());
            dto.setTipos(doc.getTipos() == null ? List.of() : doc.getTipos());
        }
        return dto;
    }

    private static List<String> limpiar(List<String> valores) {
        return valores.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
