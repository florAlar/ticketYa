package notificaciones.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import notificaciones.clients.PreferenciasClient;
import notificaciones.clients.PreferenciasClient.MatchingClientesRequest;
import notificaciones.clients.PreferenciasClient.PreferenciasNotificaciones;
import notificaciones.clients.UsuariosClient;
import notificaciones.clients.VentasClient;
import notificaciones.dto.NotificacionRequestDTO;
import notificaciones.entity.Notificacion;
import notificaciones.entity.PlantillaNotificacion;
import notificaciones.messaging.CompraConfirmadaDTO;
import notificaciones.messaging.EventoCambioDTO;
import notificaciones.observability.Bitacora;
import notificaciones.repository.NotificacionRepository;
import notificaciones.repository.PlantillaNotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

    static final String TIPO_EVENTO_CANCELADO = "EventoCancelado";
    static final String TIPO_EVENTO_SUSPENDIDO = "EventoSuspendido";
    static final String TIPO_EVENTO_MODIFICADO = "EventoModificado";
    static final String TIPO_EVENTO_PUBLICADO = "EventoPublicado";
    static final String TIPO_ARTISTA_FAVORITO = "NuevoEventoArtistaFavorito";
    static final String TIPO_COMPRA_CONFIRMADA = "CompraConfirmada";

    static final String CANAL_PUSH = "PUSH";
    static final String CANAL_EMAIL = "EMAIL";
    static final String ESTADO_PENDIENTE = "PENDIENTE";

    /** Canales que se intentan para avisos de evento (si hay plantilla activa para cada uno). */
    static final List<String> CANALES_EVENTO = List.of(CANAL_PUSH, CANAL_EMAIL);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionRepository notificacionRepository;
    private final PlantillaNotificacionRepository plantillaRepository;
    private final VentasClient ventasClient;
    private final PreferenciasClient preferenciasClient;
    private final UsuariosClient usuariosClient;

    public NotificacionServiceImpl(
            NotificacionRepository notificacionRepository,
            PlantillaNotificacionRepository plantillaRepository,
            VentasClient ventasClient,
            PreferenciasClient preferenciasClient,
            UsuariosClient usuariosClient) {
        this.notificacionRepository = notificacionRepository;
        this.plantillaRepository = plantillaRepository;
        this.ventasClient = ventasClient;
        this.preferenciasClient = preferenciasClient;
        this.usuariosClient = usuariosClient;
    }

    @Override
    @Transactional
    public void procesarEvento(EventoCambioDTO evento) {
        if (evento == null || evento.tipo() == null || evento.tipo().isBlank()) {
            return;
        }
        switch (evento.tipo()) {
            case TIPO_EVENTO_CANCELADO, TIPO_EVENTO_SUSPENDIDO, TIPO_EVENTO_MODIFICADO ->
                    notificarCompradores(evento);
            case TIPO_EVENTO_PUBLICADO -> notificarArtistasFavoritos(evento);
            default -> log.debug("Evento de dominio sin acción de notificación: {}", evento.tipo());
        }
    }

    /** Cancelación, suspensión o modificación: fan-out a clientes con compra APROBADA. */
    private void notificarCompradores(EventoCambioDTO evento) {
        List<String> clientes = ventasClient.clientesAprobadosPorEvento(evento.eventoId());
        if (clientes == null || clientes.isEmpty()) {
            log.info("Evento {} sin clientes con compra aprobada: no se generan notificaciones",
                    evento.eventoId());
            return;
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        int creadas = 0;
        for (String canal : CANALES_EVENTO) {
            Optional<PlantillaNotificacion> plantillaOpt = plantillaRepository
                    .findFirstByTipoEventoAndCanalAndActivaTrue(evento.tipo(), canal);
            if (plantillaOpt.isEmpty()) {
                log.debug("Sin plantilla activa para {} y canal {}", evento.tipo(), canal);
                continue;
            }
            PlantillaNotificacion plantilla = plantillaOpt.get();
            String clave = claveIdempotencia(evento.tipo(), canal, evento.eventoId());
            for (String idCliente : clientes) {
                if (idCliente == null || idCliente.isBlank()) {
                    continue;
                }
                if (notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(idCliente, clave)) {
                    continue;
                }
                if (!permiteNotificar(idCliente, plantilla.getCanal(), evento.tipo())) {
                    continue;
                }
                notificacionRepository.save(
                        nuevaNotificacion(idCliente, evento, plantilla, clave, ahora));
                creadas++;
            }
        }
        log.info("Evento {} ({}): {} notificación(es) creada(s) de {} cliente(s) (canales {})",
                evento.eventoId(), evento.tipo(), creadas, clientes.size(), CANALES_EVENTO);
    }

    /**
     * Evento publicado: cruza artistas + ciudad contra msvc-preferencias y notifica a los clientes
     * que siguen a algún artista del evento y viven en la ciudad donde se presenta.
     */
    private void notificarArtistasFavoritos(EventoCambioDTO evento) {
        List<String> artistas = evento.artistas() == null
                ? List.of()
                : evento.artistas().stream()
                        .filter(a -> a != null && !a.isBlank())
                        .toList();
        if (evento.ciudad() == null
                || evento.ciudad().isBlank()
                || artistas.isEmpty()) {
            log.info("Evento {} publicado sin ciudad o artistas: no hay matching de favoritos",
                    evento.eventoId());
            return;
        }

        List<String> clientes = preferenciasClient.clientesInteresados(
                new MatchingClientesRequest(evento.ciudad(), artistas));
        if (clientes == null || clientes.isEmpty()) {
            log.info("Evento {} en {} sin clientes con artistas favoritos: {}",
                    evento.eventoId(), evento.ciudad(), artistas);
            return;
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        int creadas = 0;
        for (String canal : CANALES_EVENTO) {
            Optional<PlantillaNotificacion> plantillaOpt = plantillaRepository
                    .findFirstByTipoEventoAndCanalAndActivaTrue(TIPO_ARTISTA_FAVORITO, canal);
            if (plantillaOpt.isEmpty()) {
                log.debug("Sin plantilla activa para {} y canal {}", TIPO_ARTISTA_FAVORITO, canal);
                continue;
            }
            PlantillaNotificacion plantilla = plantillaOpt.get();
            String clave = claveIdempotencia(TIPO_ARTISTA_FAVORITO, canal, evento.eventoId());
            for (String idCliente : clientes) {
                if (idCliente == null || idCliente.isBlank()) {
                    continue;
                }
                if (notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(idCliente, clave)) {
                    continue;
                }
                if (!permiteNotificar(idCliente, plantilla.getCanal(), TIPO_ARTISTA_FAVORITO)) {
                    continue;
                }
                notificacionRepository.save(
                        nuevaNotificacion(idCliente, evento, plantilla, clave, ahora));
                creadas++;
            }
        }
        log.info("Evento {} publicado en {}: {} notificación(es) de artista favorito de {} cliente(s) (canales {})",
                evento.eventoId(), evento.ciudad(), creadas, clientes.size(), CANALES_EVENTO);
    }

    @Override
    @Transactional
    public void procesarCompraConfirmada(CompraConfirmadaDTO compra) {
        if (compra == null
                || compra.clienteId() == null
                || compra.clienteId().isBlank()
                || compra.compraId() == null
                || compra.compraId().isBlank()) {
            log.warn("Compra confirmada incompleta, se ignora: {}", compra);
            return;
        }
        // La compra es el origen de la idempotencia: un cliente puede comprar dos veces el mismo evento.
        String clave = claveIdempotencia(TIPO_COMPRA_CONFIRMADA, CANAL_EMAIL, compra.compraId());
        if (notificacionRepository.existsByIdUsuarioAndClaveIdempotencia(
                compra.clienteId(), clave)) {
            log.debug("Compra {} ya notificada al cliente {}", compra.compraId(), compra.clienteId());
            return;
        }

        PlantillaNotificacion plantilla = obtenerPlantilla(TIPO_COMPRA_CONFIRMADA, CANAL_EMAIL);
        if (!permiteNotificar(compra.clienteId(), plantilla.getCanal(), TIPO_COMPRA_CONFIRMADA)) {
            log.info("Cliente {} prefiere no recibir {} por {}",
                    compra.clienteId(), TIPO_COMPRA_CONFIRMADA, plantilla.getCanal());
            return;
        }

        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("compraId", compra.compraId());
        valores.put("eventoId", compra.eventoId());
        valores.put("cantidadEntradas", compra.cantidadEntradas() == null
                ? "?"
                : String.valueOf(compra.cantidadEntradas()));
        valores.put("total", compra.total() == null ? "?" : compra.total().toPlainString());
        valores.put("moneda", compra.moneda() == null ? "" : compra.moneda());

        Notificacion n = nuevaNotificacion(
                compra.clienteId(), plantilla, compra.eventoId(), clave, valores);
        notificacionRepository.save(n);
        log.info("Compra {} confirmada: notificación {} creada para cliente {}",
                compra.compraId(), n.getIdNotificacion(), compra.clienteId());
    }

    @Override
    @Transactional
    public Notificacion crearAdministrativa(NotificacionRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        Notificacion n = new Notificacion();
        n.setIdNotificacion(UUID.randomUUID().toString());
        n.setIdUsuario(request.getIdUsuario());
        n.setIdEventoOrigen(request.getIdEventoOrigen());
        n.setTipo(request.getTipo());
        n.setCanal(request.getCanal().toUpperCase(Locale.ROOT));
        n.setAsunto(request.getAsunto());
        n.setContenido(request.getContenido());
        n.setFechaCreacion(request.getFechaCreacion() == null
                ? LocalDateTime.now(ZoneOffset.UTC)
                : request.getFechaCreacion());
        n.setEstado(ESTADO_PENDIENTE);
        n.setLeida(Boolean.FALSE);
        if (CANAL_EMAIL.equalsIgnoreCase(n.getCanal())) {
            n.setDestinatario(emailDe(request.getIdUsuario()));
        }
        Notificacion guardada = notificacionRepository.save(n);
        Bitacora.registrar(
                "NOTIFICACION_ADMIN_CREADA", null, guardada.getIdNotificacion(), "tipo=" + n.getTipo());
        return guardada;
    }

    private PlantillaNotificacion obtenerPlantilla(String tipoEvento, String canal) {
        return plantillaRepository
                .findFirstByTipoEventoAndCanalAndActivaTrue(tipoEvento, canal)
                .or(() -> plantillaRepository.findFirstByTipoEventoAndActivaTrue(tipoEvento))
                .orElseThrow(() -> new IllegalStateException(
                        "No hay plantilla activa para " + tipoEvento));
    }

    private Notificacion nuevaNotificacion(
            String idCliente,
            EventoCambioDTO evento,
            PlantillaNotificacion plantilla,
            String clave,
            LocalDateTime ahora) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("evento", evento.nombre());
        valores.put("fecha", evento.fecha() == null ? "" : FECHA.format(evento.fecha()));
        valores.put("ciudad", evento.ciudad());
        valores.put("cambios", evento.cambios());
        return nuevaNotificacion(
                idCliente, plantilla, evento.eventoId(), clave, valores, ahora);
    }

    private Notificacion nuevaNotificacion(
            String idCliente,
            PlantillaNotificacion plantilla,
            String idEvento,
            String clave,
            Map<String, String> valores) {
        return nuevaNotificacion(
                idCliente, plantilla, idEvento, clave, valores, LocalDateTime.now(ZoneOffset.UTC));
    }

    private Notificacion nuevaNotificacion(
            String idCliente,
            PlantillaNotificacion plantilla,
            String idEvento,
            String clave,
            Map<String, String> valores,
            LocalDateTime ahora) {
        Notificacion n = new Notificacion();
        n.setIdNotificacion(UUID.randomUUID().toString());
        n.setIdUsuario(idCliente);
        n.setIdEventoOrigen(idEvento);
        n.setClaveIdempotencia(clave);
        n.setPlantilla(plantilla);
        n.setTipo(plantilla.getTipoEvento());
        n.setCanal(plantilla.getCanal());
        n.setAsunto(render(plantilla.getAsunto(), valores));
        n.setContenido(render(plantilla.getContenido(), valores));
        n.setFechaCreacion(ahora);
        n.setEstado(ESTADO_PENDIENTE);
        n.setLeida(Boolean.FALSE);
        if (CANAL_EMAIL.equalsIgnoreCase(n.getCanal())) {
            n.setDestinatario(emailDe(idCliente));
        }
        return n;
    }

    /**
     * Resuelve el email del cliente en msvc-usuarios. Si no responde se deja null y el proveedor
     * SMTP usa el destinatario por defecto (MailHog lo muestra igual).
     */
    private String emailDe(String idUsuario) {
        try {
            UsuariosClient.UsuarioEmailDTO usuario = usuariosClient.porId(idUsuario);
            return usuario == null ? null : usuario.email();
        } catch (RuntimeException ex) {
            log.warn("No se pudo resolver el email de {}: {}", idUsuario, ex.getMessage());
            return null;
        }
    }

    /**
     * Consulta las preferencias de canal del cliente. Si preferencias no responde se envía igual
     * (fallback permisivo): es preferible avisar de más que perder un aviso de cancelación.
     */
    private boolean permiteNotificar(String idCliente, String canal, String tipo) {
        try {
            PreferenciasNotificaciones prefs =
                    preferenciasClient.preferenciasNotificaciones(idCliente);
            return prefs == null || (prefs.permiteCanal(canal) && prefs.permiteTipo(tipo));
        } catch (RuntimeException ex) {
            log.warn("No se pudieron leer preferencias de {} ({}). Se envía igual",
                    idCliente, ex.getMessage());
            return true;
        }
    }

    private static String claveIdempotencia(String tipo, String canal, String origen) {
        return tipo + ":" + canal + ":" + (origen == null ? "" : origen);
    }

    private static String render(String plantilla, Map<String, String> valores) {
        if (plantilla == null) {
            return "";
        }
        String resultado = plantilla;
        for (Map.Entry<String, String> e : valores.entrySet()) {
            resultado = resultado.replace(
                    "{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return resultado;
    }
}
