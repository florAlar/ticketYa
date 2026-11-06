package ventas.dto;

/**
 * Pago mock. {@code resultado}: APROBAR (default) | RECHAZAR.
 * Opcional {@code proveedor} (default MOCK_PASARELA).
 */
public record PagoCreateRequestDTO(String resultado, String proveedor) {
}
