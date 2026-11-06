package ventas.service;

import java.util.List;
import ventas.dto.CompraCreateRequestDTO;
import ventas.dto.CompraResponseDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.PagoCreateRequestDTO;
import ventas.dto.PagoResponseDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;

public interface VentaService {

    DisponibilidadResponseDTO disponibilidad(String idEvento);

    ReservaResponseDTO crearReserva(String idCliente, ReservaCreateRequestDTO request, String idempotencyKey);

    ReservaResponseDTO obtenerReserva(String idCliente, String idReserva);

    ReservaResponseDTO confirmarReserva(String idCliente, String idReserva);

    ReservaResponseDTO cancelarReserva(String idCliente, String idReserva);

    ReservaResponseDTO renovarReserva(String idCliente, String idReserva);

    CompraResponseDTO crearCompra(String idCliente, CompraCreateRequestDTO request, String idempotencyKey);

    CompraResponseDTO obtenerCompra(String idCliente, String idCompra);

    List<CompraResponseDTO> listarCompras(String idCliente);

    PagoResponseDTO iniciarPago(String idCliente, String idCompra, PagoCreateRequestDTO request);

    List<PagoResponseDTO> listarPagos(String idCliente, String idCompra);

    PagoResponseDTO reintentarPago(String idCliente, String idPago, PagoCreateRequestDTO request);

    /** Expira reservas vencidas y libera cupo (scheduler + lazy). */
    int expirarReservasVencidas();
}
