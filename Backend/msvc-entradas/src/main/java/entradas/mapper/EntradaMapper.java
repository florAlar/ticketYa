package entradas.mapper;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;
import entradas.entity.Devolucion;
import entradas.entity.Entrada;

@Component
public class EntradaMapper {

    public EntradaResponseDTO toEntrada(Entrada e) {
        return toEntrada(e, null);
    }

    public EntradaResponseDTO toEntrada(Entrada e, BigDecimal precio) {
        EntradaResponseDTO dto = new EntradaResponseDTO();
        dto.setId(e.getIdEntrada());
        dto.setCodigoQR(e.getCodigoQr());
        dto.setIdCompra(e.getIdCompra());
        dto.setIdCliente(e.getIdCliente());
        dto.setIdEvento(e.getIdEvento());
        dto.setIdSector(e.getIdSector());
        dto.setEstado(e.getEstado());
        dto.setFechaEmision(e.getFechaEmision());
        dto.setFechaIngreso(e.getFechaValidacion());
        dto.setPrecio(precio);
        return dto;
    }

    public QrResponseDTO toQr(Entrada e) {
        QrResponseDTO dto = new QrResponseDTO();
        dto.setId(e.getIdEntrada());
        dto.setCodigoQR(e.getCodigoQr());
        return dto;
    }

    public DevolucionResponseDTO toDevolucion(Devolucion d) {
        DevolucionResponseDTO dto = new DevolucionResponseDTO();
        dto.setId(d.getIdDevolucion());
        dto.setIdEntrada(d.getEntrada().getIdEntrada());
        dto.setIdCliente(d.getEntrada().getIdCliente());
        dto.setMotivo(d.getMotivo());
        dto.setMonto(d.getMonto());
        dto.setFechaSolicitud(d.getFechaSolicitud());
        dto.setEstado(d.getEstado());
        return dto;
    }
}
