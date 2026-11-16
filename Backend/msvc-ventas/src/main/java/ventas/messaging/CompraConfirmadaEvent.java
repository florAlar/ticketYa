package ventas.messaging;

import java.math.BigDecimal;

/** Evento de Spring que dispara la notificación de compra confirmada (se publica al broker tras el commit). */
public record CompraConfirmadaEvent(
        String compraId,
        String clienteId,
        String eventoId,
        BigDecimal total,
        String moneda,
        Integer cantidadEntradas) {
}
