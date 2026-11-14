package notificaciones.service;

import notificaciones.messaging.EventoCambioDTO;

public interface NotificacionService {

    /**
     * Procesa un evento de dominio de msvc-eventos y genera las notificaciones push
     * correspondientes a los clientes afectados. Idempotente por (usuario, evento, tipo).
     */
    void procesarEvento(EventoCambioDTO evento);
}
