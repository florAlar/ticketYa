package notificaciones.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import notificaciones.clients.VentasClient;
import notificaciones.entity.Notificacion;
import notificaciones.entity.PlantillaNotificacion;
import notificaciones.messaging.EventoCambioDTO;
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
    static final String CANAL_PUSH = "PUSH";
    static final String ESTADO_PENDIENTE = "PENDIENTE";

    private final NotificacionRepository notificacionRepository;
    private final PlantillaNotificacionRepository plantillaRepository;
    private final VentasClient ventasClient;

    public NotificacionServiceImpl(
            NotificacionRepository notificacionRepository,
            PlantillaNotificacionRepository plantillaRepository,
            VentasClient ventasClient) {
        this.notificacionRepository = notificacionRepository;
        this.plantillaRepository = plantillaRepository;
        this.ventasClient = ventasClient;
    }

    @Override
    @Transactional
    public void procesarEvento(EventoCambioDTO evento) {
        if (evento == null || !TIPO_EVENTO_CANCELADO.equals(evento.tipo())) {
            return;
        }

        List<String> clientes = ventasClient.clientesAprobadosPorEvento(evento.eventoId());
        if (clientes == null || clientes.isEmpty()) {
            log.info("Evento {} sin clientes con compra aprobada: no se generan notificaciones",
                    evento.eventoId());
            return;
        }

        PlantillaNotificacion plantilla = plantillaRepository
                .findFirstByTipoEventoAndCanalAndActivaTrue(TIPO_EVENTO_CANCELADO, CANAL_PUSH)
                .or(() -> plantillaRepository.findFirstByTipoEventoAndActivaTrue(TIPO_EVENTO_CANCELADO))
                .orElseThrow(() -> new IllegalStateException(
                        "No hay plantilla activa para " + TIPO_EVENTO_CANCELADO));

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        int creadas = 0;
        for (String idCliente : clientes) {
            if (idCliente == null || idCliente.isBlank()) {
                continue;
            }
            if (notificacionRepository.existsByIdUsuarioAndIdEventoOrigenAndTipo(
                    idCliente, evento.eventoId(), TIPO_EVENTO_CANCELADO)) {
                continue;
            }
            notificacionRepository.save(nuevaNotificacion(idCliente, evento, plantilla, ahora));
            creadas++;
        }
        log.info("Evento {} ({}): {} notificación(es) push creada(s) de {} cliente(s)",
                evento.eventoId(), evento.tipo(), creadas, clientes.size());
    }

    private Notificacion nuevaNotificacion(
            String idCliente,
            EventoCambioDTO evento,
            PlantillaNotificacion plantilla,
            LocalDateTime ahora) {
        Notificacion n = new Notificacion();
        n.setIdNotificacion(UUID.randomUUID().toString());
        n.setIdUsuario(idCliente);
        n.setIdEventoOrigen(evento.eventoId());
        n.setPlantilla(plantilla);
        n.setTipo(TIPO_EVENTO_CANCELADO);
        n.setCanal(CANAL_PUSH);
        n.setAsunto(plantilla.getAsunto());
        n.setContenido(plantilla.getContenido());
        n.setFechaCreacion(ahora);
        n.setEstado(ESTADO_PENDIENTE);
        return n;
    }
}
