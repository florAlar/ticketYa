package ventas.service;

import java.util.List;
import ventas.dto.AbrirCuposRequestDTO;
import ventas.dto.LiberarCupoRequestDTO;
import ventas.dto.CompraCreateRequestDTO;
import ventas.dto.CompraResponseDTO;
import ventas.dto.DisponibilidadBatchResponseDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.PagoCreateRequestDTO;
import ventas.dto.PagoResponseDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;

public interface VentaService {

    DisponibilidadResponseDTO disponibilidad(String idEvento);

        /**
         * Disponibilidad comercial de varios eventos en una query.
         * ids vacíos → lista vacía. Sin cupos para un id → ítem agotado (total 0).
         */
        DisponibilidadBatchResponseDTO disponibilidadBatch(List<String> idEventos);

        /**
                 * Abre o asegura cupos comerciales para un evento (idempotente).
                 * Usado al publicar desde msvc-eventos.
                 */
                DisponibilidadResponseDTO abrirCupos(String idEvento, AbrirCuposRequestDTO request);

            /**
             * Devuelve unidades vendidas al disponible (devolución aprobada).
             * Lock pesimista sobre cupo_sector.
             */
            DisponibilidadResponseDTO liberarCupoDevolucion(String idEvento, LiberarCupoRequestDTO request);

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
