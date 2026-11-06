package ventas.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import ventas.dto.CompraResponseDTO;
import ventas.dto.DetalleReservaDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.PagoResponseDTO;
import ventas.dto.ReservaResponseDTO;
import ventas.entity.Compra;
import ventas.entity.CupoSector;
import ventas.entity.DetalleReserva;
import ventas.entity.Pago;
import ventas.entity.Reserva;

@Component
public class VentaMapper {

    public DisponibilidadResponseDTO toDisponibilidad(String idEvento, List<CupoSector> cupos) {
        DisponibilidadResponseDTO dto = new DisponibilidadResponseDTO();
        dto.setIdEvento(idEvento);
        for (CupoSector c : cupos) {
            DisponibilidadResponseDTO.CupoSectorDTO item = new DisponibilidadResponseDTO.CupoSectorDTO();
            item.setIdSector(c.getIdSector());
            item.setCapacidadTotal(c.getCapacidadTotal());
            item.setCantidadDisponible(c.getCantidadDisponible());
            item.setCantidadReservada(c.getCantidadReservada());
            item.setCantidadVendida(c.getCantidadVendida());
            dto.getSectores().add(item);
        }
        return dto;
    }

    public ReservaResponseDTO toReserva(Reserva reserva, List<DetalleReserva> detalles) {
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setId(reserva.getIdReserva());
        dto.setIdCliente(reserva.getIdCliente());
        dto.setIdEvento(reserva.getIdEvento());
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setFechaExpiracion(reserva.getFechaExpiracion());
        dto.setEstado(reserva.getEstado());
        dto.setTotal(reserva.getTotal());
        for (DetalleReserva d : detalles) {
            DetalleReservaDTO item = new DetalleReservaDTO();
            item.setIdDetalle(d.getIdDetalle());
            item.setIdSector(d.getIdSector());
            item.setCantidad(d.getCantidad());
            item.setPrecioUnitario(d.getPrecioUnitario());
            dto.getDetalles().add(item);
        }
        return dto;
    }

    public CompraResponseDTO toCompra(Compra compra) {
        CompraResponseDTO dto = new CompraResponseDTO();
        dto.setId(compra.getIdCompra());
        dto.setIdReserva(compra.getReserva() != null ? compra.getReserva().getIdReserva() : null);
        dto.setIdCliente(compra.getIdCliente());
        dto.setFechaCompra(compra.getFechaCompra());
        dto.setTotal(compra.getTotal());
        dto.setMoneda(compra.getMoneda());
        dto.setEstado(compra.getEstado());
        return dto;
    }

    public PagoResponseDTO toPago(Pago pago) {
        PagoResponseDTO dto = new PagoResponseDTO();
        dto.setId(pago.getIdPago());
        dto.setIdCompra(pago.getCompra().getIdCompra());
        dto.setProveedor(pago.getProveedor());
        dto.setIdTransaccionExterna(pago.getIdTransaccionExterna());
        dto.setMonto(pago.getMonto());
        dto.setEstado(pago.getEstado());
        dto.setFechaPago(pago.getFechaPago());
        return dto;
    }
}
