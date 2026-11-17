package preferencias.service;

import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import preferencias.clients.EventosClient;
import preferencias.clients.EventosClient.EventoDetalleDTO;
import preferencias.clients.EventosClient.EventoResumenDTO;
import preferencias.clients.EventosClient.ParticipanteDTO;
import preferencias.config.RecomendacionProperties;
import preferencias.document.InteraccionEvento;
import preferencias.document.PreferenciasCliente;
import preferencias.document.RangoPrecio;
import preferencias.dto.EventoRecomendadoDTO;
import preferencias.exception.ResourceNotFoundException;
import preferencias.observability.Bitacora;
import preferencias.repository.PreferenciasClienteRepository;

@Service
public class RecomendacionServiceImpl implements RecomendacionService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionServiceImpl.class);

    private final EventosClient eventosClient;
    private final InteraccionService interaccionService;
    private final PreferenciasClienteRepository preferenciasRepository;
    private final RecomendacionProperties props;

    public RecomendacionServiceImpl(
            EventosClient eventosClient,
            InteraccionService interaccionService,
            PreferenciasClienteRepository preferenciasRepository,
            RecomendacionProperties props) {
        this.eventosClient = eventosClient;
        this.interaccionService = interaccionService;
        this.preferenciasRepository = preferenciasRepository;
        this.props = props;
    }

    @Override
    public List<EventoRecomendadoDTO> similares(String idEvento, int limit) {
        if (idEvento == null || idEvento.isBlank()) {
            throw new IllegalArgumentException("idEvento es obligatorio");
        }
        String eventoId = idEvento.trim();
        int limite = limitar(limit, props.getLimiteSimilares());

        EventoDetalleDTO fuente;
        try {
            fuente = eventosClient.getEvento(eventoId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Evento inexistente: " + eventoId);
        } catch (RuntimeException ex) {
            log.warn("No se pudo obtener el evento {} para recomendaciones: {}",
                    eventoId, ex.getMessage());
            return List.of();
        }
        if (fuente == null) {
            throw new ResourceNotFoundException("Evento inexistente: " + eventoId);
        }

        List<EventoResumenDTO> candidatos = candidatos();
        if (candidatos.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> coCompras = coComprasDeEvento(eventoId);
        Set<String> artistasFuente = artistasDe(fuente);
        String tipoFuente = normalizar(fuente.getTipo());
        String ciudadFuente = normalizar(ciudadDe(fuente));
        RecomendacionProperties.Pesos pesos = props.getPesos();

        Map<String, Puntaje> puntajes = new HashMap<>();
        for (EventoResumenDTO c : candidatos) {
            if (c.getId() == null || c.getId().equals(eventoId)) {
                continue;
            }
            int co = coCompras.getOrDefault(c.getId(), 0);
            int artistasComunes = contarComunes(artistasFuente, c.getArtistas());
            boolean mismoTipo = !tipoFuente.isEmpty() && tipoFuente.equals(normalizar(c.getTipo()));
            boolean mismaCiudad = !ciudadFuente.isEmpty() && ciudadFuente.equals(normalizar(ciudadDe(c)));

            double score = Math.min(co, pesos.getCoCompraTope()) * pesos.getCoCompra();
            if (mismoTipo) {
                score += pesos.getCategoria();
            }
            if (mismaCiudad) {
                score += pesos.getCiudad();
            }
            score += artistasComunes * pesos.getArtista();
            if (score <= 0) {
                continue;
            }

            String motivo;
            if (co > 0) {
                motivo = "Quienes compraron este evento también compraron este";
            } else if (artistasComunes > 0) {
                motivo = "Comparte artistas con " + nombreDe(fuente);
            } else if (mismoTipo) {
                motivo = "Mismo tipo de evento";
            } else {
                motivo = "En la misma ciudad";
            }
            puntajes.put(c.getId(), new Puntaje(score, motivo));
        }

        List<EventoRecomendadoDTO> resultado =
                rankear(candidatos, puntajes, limite, Set.of(eventoId));
        Bitacora.registrar(
                "RECOMENDACIONES_SIMILARES",
                null,
                eventoId,
                "candidatos=" + candidatos.size() + " devueltos=" + resultado.size());
        return resultado;
    }

    @Override
    public List<EventoRecomendadoDTO> paraCliente(String idCliente, int limit) {
        if (idCliente == null || idCliente.isBlank()) {
            throw new IllegalArgumentException("idCliente es obligatorio");
        }
        String clienteId = idCliente.trim();
        int limite = limitar(limit, props.getLimiteMe());
        List<EventoResumenDTO> candidatos = candidatos();
        if (candidatos.isEmpty()) {
            return List.of();
        }

        PreferenciasCliente prefs =
                preferenciasRepository.findByIdCliente(clienteId).orElse(null);
        List<InteraccionEvento> historial = interaccionService.comprasDeCliente(clienteId);
        Set<String> comprados = historial.stream()
                .map(InteraccionEvento::getIdEvento)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Integer> coCompras = coComprasDeCliente(clienteId, historial);
        RecomendacionProperties.Pesos pesos = props.getPesos();
        List<String> categorias = prefs == null ? List.of() : prefs.getCategorias();
        List<String> ciudades = prefs == null ? List.of() : prefs.getCiudades();
        List<String> artistasFavoritos = prefs == null ? List.of() : prefs.getArtistas();
        RangoPrecio rango = prefs == null ? null : prefs.getRangoPrecio();

        Map<String, Puntaje> puntajes = new HashMap<>();
        for (EventoResumenDTO c : candidatos) {
            if (c.getId() == null || comprados.contains(c.getId())) {
                continue;
            }
            int co = coCompras.getOrDefault(c.getId(), 0);
            String artista = primerArtistaComun(artistasFavoritos, c.getArtistas());
            boolean mismaCategoria = contieneIgnorandoMayusculas(categorias, c.getTipo());
            boolean mismaCiudad = contieneCiudad(ciudades, c);
            boolean precioOk = rangoContiene(rango, c.getPrecioDesde());

            double score = Math.min(co, pesos.getCoCompraTope()) * pesos.getCoCompra();
            if (artista != null) {
                score += pesos.getArtista();
            }
            if (mismaCategoria) {
                score += pesos.getCategoria();
            }
            if (mismaCiudad) {
                score += pesos.getCiudad();
            }
            if (precioOk) {
                score += pesos.getPrecio();
            }
            if (c.isEsAltaDemanda()) {
                score += pesos.getDemanda();
            }
            if (score <= 0) {
                continue;
            }

            String motivo;
            if (artista != null) {
                motivo = "Porque te gusta " + artista;
            } else if (co > 0) {
                motivo = "Clientes con tus mismos gustos también compraron este evento";
            } else if (mismaCategoria) {
                motivo = "Porque te gustan los eventos de tipo " + c.getTipo();
            } else if (mismaCiudad) {
                motivo = "Porque es en " + ciudadDe(c);
            } else if (precioOk) {
                motivo = "Dentro de tu rango de precio";
            } else {
                motivo = "Evento con alta demanda";
            }
            puntajes.put(c.getId(), new Puntaje(score, motivo));
        }

        List<EventoRecomendadoDTO> resultado = rankear(candidatos, puntajes, limite, comprados);
        Bitacora.registrar(
                "RECOMENDACIONES_CLIENTE",
                clienteId,
                clienteId,
                "candidatos=" + candidatos.size() + " historial=" + comprados.size()
                        + " devueltos=" + resultado.size());
        return resultado;
    }

    private List<EventoResumenDTO> candidatos() {
        try {
            EventosClient.EventosPageDTO page = eventosClient.listar(
                    "PUBLICADO",
                    LocalDate.now(ZoneOffset.UTC).toString(),
                    props.getCandidatosMaximos(),
                    true);
            if (page == null || page.getContent() == null) {
                return List.of();
            }
            Map<String, EventoResumenDTO> unique = new LinkedHashMap<>();
            for (EventoResumenDTO c : page.getContent()) {
                if (c != null && c.getId() != null && !c.getId().isBlank()) {
                    unique.putIfAbsent(c.getId(), c);
                }
            }
            return new ArrayList<>(unique.values());
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar el catálogo de eventos para recomendaciones: {}",
                    ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Integer> coComprasDeEvento(String idEvento) {
        List<InteraccionEvento> compras = interaccionService.comprasDeEvento(idEvento);
        Set<String> clientes = compras.stream()
                .map(InteraccionEvento::getIdCliente)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (clientes.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        Set<String> vistos = new HashSet<>();
        for (InteraccionEvento otra : interaccionService.comprasDeClientes(clientes)) {
            String evento = otra.getIdEvento();
            if (evento != null && !evento.equals(idEvento)
                    && vistos.add(otra.getIdCliente() + "|" + evento)) {
                counts.merge(evento, 1, Integer::sum);
            }
        }
        return counts;
    }

    private Map<String, Integer> coComprasDeCliente(
            String idCliente, List<InteraccionEvento> historial) {
        Set<String> misEventos = historial.stream()
                .map(InteraccionEvento::getIdEvento)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (misEventos.isEmpty()) {
            return Map.of();
        }
        Set<String> otrosClientes = interaccionService.comprasDeEventos(misEventos).stream()
                .map(InteraccionEvento::getIdCliente)
                .filter(Objects::nonNull)
                .filter(c -> !c.equals(idCliente))
                .collect(Collectors.toSet());
        if (otrosClientes.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        Set<String> vistos = new HashSet<>();
        for (InteraccionEvento otra : interaccionService.comprasDeClientes(otrosClientes)) {
            String evento = otra.getIdEvento();
            if (evento != null
                    && !misEventos.contains(evento)
                    && !idCliente.equals(otra.getIdCliente())
                    && vistos.add(otra.getIdCliente() + "|" + evento)) {
                counts.merge(evento, 1, Integer::sum);
            }
        }
        return counts;
    }

    private List<EventoRecomendadoDTO> rankear(
            List<EventoResumenDTO> candidatos,
            Map<String, Puntaje> puntajes,
            int limite,
            Set<String> excluir) {
        Map<String, EventoResumenDTO> porId = candidatos.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(
                        EventoResumenDTO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<String> ordenados = puntajes.entrySet().stream()
                .sorted(Comparator
                        .comparingDouble((Map.Entry<String, Puntaje> e) -> e.getValue().score())
                        .reversed()
                        .thenComparing(e -> fechaDe(porId.get(e.getKey())),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Map.Entry::getKey)
                .toList();

        List<EventoRecomendadoDTO> resultado = new ArrayList<>();
        LinkedHashSet<String> incluidos = new LinkedHashSet<>();
        Deque<EventoResumenDTO> fallback = new ArrayDeque<>(ordenFallback(candidatos, excluir));

        for (String id : ordenados) {
            if (excluir != null && excluir.contains(id)) {
                continue;
            }
            EventoResumenDTO c = porId.get(id);
            if (c == null) {
                continue;
            }
            resultado.add(toDto(c, puntajes.get(id).motivo()));
            incluidos.add(id);
            if (resultado.size() >= limite) {
                return resultado;
            }
        }

        while (resultado.size() < limite && !fallback.isEmpty()) {
            EventoResumenDTO c = fallback.poll();
            if (c.getId() == null
                    || incluidos.contains(c.getId())
                    || (excluir != null && excluir.contains(c.getId()))) {
                continue;
            }
            resultado.add(toDto(c, motivoFallback(c)));
            incluidos.add(c.getId());
        }
        return resultado;
    }

    private List<EventoResumenDTO> ordenFallback(List<EventoResumenDTO> candidatos, Set<String> excluir) {
        return candidatos.stream()
                .filter(c -> c.getId() != null)
                .filter(c -> excluir == null || !excluir.contains(c.getId()))
                .filter(this::esFuturo)
                .sorted(Comparator
                        .comparing(EventoResumenDTO::isEsAltaDemanda).reversed()
                        .thenComparing(c -> fechaDe(c),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private EventoRecomendadoDTO toDto(EventoResumenDTO c, String motivo) {
        EventoRecomendadoDTO dto = new EventoRecomendadoDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setTipo(c.getTipo());
        dto.setFechaInicio(c.getFechaInicio());
        if (c.getLugar() != null) {
            EventoRecomendadoDTO.LugarResumenDTO lugar = new EventoRecomendadoDTO.LugarResumenDTO();
            lugar.setNombre(c.getLugar().getNombre());
            lugar.setCiudad(c.getLugar().getCiudad());
            dto.setLugar(lugar);
        }
        dto.setImagenUrl(c.getImagenUrl());
        dto.setPrecioDesde(c.getPrecioDesde());
        dto.setDestacado(c.isDestacado());
        dto.setEstado(c.getEstado());
        dto.setEsAltaDemanda(c.isEsAltaDemanda());
        dto.setArtistas(c.getArtistas() == null ? List.of() : c.getArtistas());
        dto.setMotivo(motivo);
        return dto;
    }

    private static String motivoFallback(EventoResumenDTO c) {
        return c.isEsAltaDemanda() ? "Evento con alta demanda" : "Próximo evento publicado";
    }

    private boolean esFuturo(EventoResumenDTO c) {
        LocalDateTime fecha = fechaDe(c);
        return fecha == null || !fecha.isBefore(LocalDateTime.now(ZoneOffset.UTC).minusHours(2));
    }

    private static LocalDateTime fechaDe(EventoResumenDTO c) {
        return c == null ? null : c.getFechaInicio();
    }

    private int limitar(int limit, int defecto) {
        int valor = limit <= 0 ? defecto : limit;
        return Math.max(1, Math.min(valor, props.getLimiteMaximo()));
    }

    private static Set<String> artistasDe(EventoDetalleDTO evento) {
        if (evento.getParticipantes() == null) {
            return Set.of();
        }
        return evento.getParticipantes().stream()
                .map(ParticipanteDTO::getNombre)
                .filter(Objects::nonNull)
                .map(RecomendacionServiceImpl::normalizar)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static int contarComunes(Set<String> favoritos, List<String> artistasEvento) {
        if (favoritos.isEmpty() || artistasEvento == null || artistasEvento.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (String artista : artistasEvento) {
            if (favoritos.contains(normalizar(artista))) {
                n++;
            }
        }
        return n;
    }

    private static String primerArtistaComun(List<String> favoritos, List<String> artistasEvento) {
        if (favoritos == null || favoritos.isEmpty() || artistasEvento == null || artistasEvento.isEmpty()) {
            return null;
        }
        Set<String> delEvento = artistasEvento.stream()
                .filter(Objects::nonNull)
                .map(RecomendacionServiceImpl::normalizar)
                .collect(Collectors.toSet());
        for (String favorito : favoritos) {
            if (favorito != null && delEvento.contains(normalizar(favorito))) {
                return favorito.trim();
            }
        }
        return null;
    }

    private static boolean contieneIgnorandoMayusculas(List<String> valores, String valor) {
        if (valores == null || valores.isEmpty() || valor == null || valor.isBlank()) {
            return false;
        }
        String objetivo = normalizar(valor);
        return valores.stream()
                .filter(Objects::nonNull)
                .map(RecomendacionServiceImpl::normalizar)
                .anyMatch(objetivo::equals);
    }

    private static boolean contieneCiudad(List<String> ciudades, EventoResumenDTO c) {
        String ciudad = ciudadDe(c);
        return contieneIgnorandoMayusculas(ciudades, ciudad);
    }

    private static boolean rangoContiene(RangoPrecio rango, java.math.BigDecimal precio) {
        if (rango == null || precio == null) {
            return false;
        }
        if (rango.getMin() == null && rango.getMax() == null) {
            return false;
        }
        if (rango.getMin() != null && precio.compareTo(rango.getMin()) < 0) {
            return false;
        }
        return rango.getMax() == null || precio.compareTo(rango.getMax()) <= 0;
    }

    private static String ciudadDe(EventoResumenDTO c) {
        return c.getLugar() == null ? null : c.getLugar().getCiudad();
    }

    private static String ciudadDe(EventoDetalleDTO e) {
        return e.getLugar() == null ? null : e.getLugar().getCiudad();
    }

    private static String nombreDe(EventoDetalleDTO e) {
        return e.getNombre() == null || e.getNombre().isBlank() ? "el evento" : e.getNombre();
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private record Puntaje(double score, String motivo) {}
}
