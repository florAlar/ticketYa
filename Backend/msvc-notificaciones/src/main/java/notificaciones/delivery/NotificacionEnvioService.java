package notificaciones.delivery;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import notificaciones.entity.IntentoEnvio;
import notificaciones.entity.Notificacion;
import notificaciones.observability.Bitacora;
import notificaciones.repository.IntentoEnvioRepository;
import notificaciones.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job de entrega: toma notificaciones {@code PENDIENTE}, las envía por el proveedor de su canal
 * y registra cada intento en {@code intento_envio}.
 *
 * <p>Estados: {@code PENDIENTE → ENVIADA | FALLIDA}. Si el envío falla se reintenta en el próximo
 * ciclo hasta {@code max-intentos}; luego queda {@code FALLIDA} para no bloquear la cola.
 */
@Service
public class NotificacionEnvioService {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ENVIADA = "ENVIADA";
    public static final String ESTADO_FALLIDA = "FALLIDA";

    static final String RESULTADO_OK = "OK";
    static final String RESULTADO_ERROR = "ERROR";

    private static final Logger log = LoggerFactory.getLogger(NotificacionEnvioService.class);

    private final NotificacionRepository notificacionRepository;
    private final IntentoEnvioRepository intentoEnvioRepository;
    private final Map<String, NotificacionProvider> providers;

    @Value("${ticketya.notificaciones.envio.max-intentos:3}")
    private int maxIntentos;

    @Value("${ticketya.notificaciones.envio.lote:50}")
    private int lote;

    public NotificacionEnvioService(
            NotificacionRepository notificacionRepository,
            IntentoEnvioRepository intentoEnvioRepository,
            List<NotificacionProvider> providers) {
        this.notificacionRepository = notificacionRepository;
        this.intentoEnvioRepository = intentoEnvioRepository;
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.canal().toUpperCase(Locale.ROOT), Function.identity()));
    }

    @Scheduled(
            fixedDelayString = "${ticketya.notificaciones.envio.intervalo-ms:30000}",
            initialDelayString = "${ticketya.notificaciones.envio.intervalo-ms:30000}")
    @Transactional
    public void procesarPendientes() {
        List<Notificacion> pendientes =
                notificacionRepository.findPendientesParaEnvio(PageRequest.of(0, lote));
        if (pendientes.isEmpty()) {
            return;
        }
        log.debug("Procesando {} notificación(es) pendiente(s)", pendientes.size());
        for (Notificacion notificacion : pendientes) {
            procesarUna(notificacion);
        }
    }

    private void procesarUna(Notificacion notificacion) {
        long intentosPrevios =
                intentoEnvioRepository.countByNotificacion_IdNotificacion(notificacion.getIdNotificacion());
        if (intentosPrevios >= maxIntentos) {
            marcarFallida(notificacion, "Máximo de intentos alcanzado (" + maxIntentos + ")");
            return;
        }

        String canal = notificacion.getCanal() == null
                ? ""
                : notificacion.getCanal().toUpperCase(Locale.ROOT);
        NotificacionProvider provider = providers.get(canal);
        if (provider == null) {
            registrarIntento(
                    notificacion,
                    (int) intentosPrevios + 1,
                    RESULTADO_ERROR,
                    "Sin proveedor configurado para canal " + canal);
            if (intentosPrevios + 1 >= maxIntentos) {
                marcarFallida(notificacion, "Sin proveedor para canal " + canal);
            }
            return;
        }

        try {
            NotificacionProvider.ResultadoEnvio resultado = provider.enviar(notificacion);
            if (resultado == null || !resultado.exito()) {
                String detalle = resultado == null ? "Resultado nulo" : resultado.detalle();
                registrarIntento(
                        notificacion, (int) intentosPrevios + 1, RESULTADO_ERROR, detalle);
                if (intentosPrevios + 1 >= maxIntentos) {
                    marcarFallida(notificacion, detalle);
                }
                return;
            }
            registrarIntento(
                    notificacion, (int) intentosPrevios + 1, RESULTADO_OK, resultado.detalle());
            notificacion.setEstado(ESTADO_ENVIADA);
            notificacion.setFechaEnvio(LocalDateTime.now(ZoneOffset.UTC));
            notificacionRepository.save(notificacion);
            Bitacora.registrar(
                    "NOTIFICACION_ENVIADA",
                    notificacion.getIdUsuario(),
                    notificacion.getIdNotificacion(),
                    "tipo=" + notificacion.getTipo() + " canal=" + canal);
        } catch (RuntimeException ex) {
            String detalle = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            registrarIntento(notificacion, (int) intentosPrevios + 1, RESULTADO_ERROR, detalle);
            if (intentosPrevios + 1 >= maxIntentos) {
                marcarFallida(notificacion, detalle);
            } else {
                log.warn(
                        "Envío falló para notificación {} (intento {}): {}. Se reintenta en el próximo ciclo",
                        notificacion.getIdNotificacion(),
                        intentosPrevios + 1,
                        detalle);
            }
        }
    }

    private void marcarFallida(Notificacion notificacion, String detalle) {
        notificacion.setEstado(ESTADO_FALLIDA);
        notificacionRepository.save(notificacion);
        Bitacora.registrar(
                "NOTIFICACION_FALLIDA",
                notificacion.getIdUsuario(),
                notificacion.getIdNotificacion(),
                detalle);
        log.warn(
                "Notificación {} quedó FALLIDA: {}",
                notificacion.getIdNotificacion(),
                detalle);
    }

    private void registrarIntento(
            Notificacion notificacion, int numeroIntento, String resultado, String detalle) {
        IntentoEnvio intento = new IntentoEnvio();
        intento.setIdIntento(UUID.randomUUID().toString());
        intento.setNotificacion(notificacion);
        intento.setNumeroIntento(numeroIntento);
        intento.setFechaIntento(LocalDateTime.now(ZoneOffset.UTC));
        intento.setResultado(resultado);
        intento.setDetalleError(truncar(detalle, 500));
        intentoEnvioRepository.save(intento);
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
