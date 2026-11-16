package notificaciones.delivery;

import notificaciones.entity.Notificacion;

/**
 * Adaptador de salida de un canal de notificación (PUSH, EMAIL, …).
 *
 * <p>Hoy solo existen implementaciones mock: simulan el envío y dejan evidencia en logs y en
 * {@code intento_envio}. Para integrar un proveedor real (FCM, WebPush, SMTP) alcanza con agregar
 * otra implementación de esta interfaz; el job de envío no cambia.
 */
public interface NotificacionProvider {

    /** Canal que atiende esta implementación: PUSH | EMAIL. */
    String canal();

    ResultadoEnvio enviar(Notificacion notificacion);

    record ResultadoEnvio(boolean exito, String detalle) {

        public static ResultadoEnvio ok(String detalle) {
            return new ResultadoEnvio(true, detalle);
        }

        public static ResultadoEnvio error(String detalle) {
            return new ResultadoEnvio(false, detalle);
        }
    }
}
