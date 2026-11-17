package notificaciones.service;

import notificaciones.dto.NotificacionRequestDTO;
import notificaciones.entity.Notificacion;
import notificaciones.messaging.CompraConfirmadaDTO;
import notificaciones.messaging.DevolucionAprobadaDTO;
import notificaciones.messaging.EventoCambioDTO;

public interface NotificacionService {

    /**
     * Procesa un evento de dominio de msvc-eventos:
     * <ul>
     *   <li>{@code EventoCancelado}, {@code EventoSuspendido}, {@code EventoModificado}: avisa a los
     *       clientes con compra APROBADA del evento.</li>
     *   <li>{@code EventoPublicado}: avisa a los clientes que tienen alguno de los artistas y la
     *       ciudad del evento entre sus preferencias.</li>
     * </ul>
     * Idempotente por (usuario, origen, tipo).
     */
    void procesarEvento(EventoCambioDTO evento);

    /** Pago aprobado en msvc-ventas → notificación de compra confirmada. Idempotente por compra. */
    void procesarCompraConfirmada(CompraConfirmadaDTO compra);

    /**
     * Devolución aprobada en msvc-entradas → aviso al cliente dueño de la entrada (PUSH + EMAIL
     * según plantillas/preferencias). Idempotente por devolución y canal.
     */
    void procesarDevolucionAprobada(DevolucionAprobadaDTO devolucion);

    /** Alta administrativa (ADMIN); queda PENDIENTE para que el job la envíe. */
    Notificacion crearAdministrativa(NotificacionRequestDTO request);
}
