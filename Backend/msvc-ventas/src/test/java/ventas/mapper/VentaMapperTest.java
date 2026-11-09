package ventas.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
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

class VentaMapperTest {

    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000011";
    private static final String ID_RESERVA = "r0000000-0000-4000-8000-000000000001";
    private static final String ID_COMPRA = "x0000000-0000-4000-8000-000000000001";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";

    private CupoSector cupo(String idSector, int disponible, int reservada, int vendida, int capacidad) {
        CupoSector c = new CupoSector();
        c.setIdSector(idSector);
        c.setCantidadDisponible(disponible);
        c.setCantidadReservada(reservada);
        c.setCantidadVendida(vendida);
        c.setCapacidadTotal(capacidad);
        return c;
    }

    private Reserva reserva(String idReserva, String idCliente, String idEvento, String estado, BigDecimal total) {
        Reserva r = new Reserva();
        r.setIdReserva(idReserva);
        r.setIdCliente(idCliente);
        r.setIdEvento(idEvento);
        r.setEstado(estado);
        r.setTotal(total);
        r.setFechaCreacion(LocalDateTime.of(2026, 3, 1, 12, 0));
        r.setFechaExpiracion(LocalDateTime.of(2026, 3, 1, 12, 10));
        return r;
    }

    @Test
    void toDisponibilidad_sinCupos_totalCeroYDisponible() {
        VentaMapper mapper = new VentaMapper();
        DisponibilidadResponseDTO dto = mapper.toDisponibilidad(ID_EVENTO, new ArrayList<>());

        assertNotNull(dto);
        assertEquals(ID_EVENTO, dto.getIdEvento());
        assertEquals(0, dto.getDisponibleTotal());
        assertEquals("AGOTADO", dto.getNivelCupo());
        assertTrue(dto.isAgotado());
        assertTrue(dto.getSectores().isEmpty());
    }

    @Test
    void toDisponibilidad_conCupos_mapeaDatos() {
        VentaMapper mapper = new VentaMapper();
        List<CupoSector> cupos = new ArrayList<>();
        cupos.add(cupo(ID_SECTOR, 50, 10, 40, 100));

        DisponibilidadResponseDTO dto = mapper.toDisponibilidad(ID_EVENTO, cupos);

        assertNotNull(dto);
        assertEquals(50, dto.getDisponibleTotal());
        assertEquals("MENOS_DE_100", dto.getNivelCupo());
        assertFalse(dto.isAgotado());
        assertEquals(1, dto.getSectores().size());
        DisponibilidadResponseDTO.CupoSectorDTO item = dto.getSectores().get(0);
        assertEquals(ID_SECTOR, item.getIdSector());
        assertEquals(Integer.valueOf(100), item.getCapacidadTotal());
        assertEquals(Integer.valueOf(50), item.getCantidadDisponible());
        assertEquals(Integer.valueOf(10), item.getCantidadReservada());
        assertEquals(Integer.valueOf(40), item.getCantidadVendida());
        assertEquals("MENOS_DE_100", item.getNivelCupo());
        assertEquals("Menos de 100", item.getNivelCupoLabel());
    }

    @Test
    void toDisponibilidad_cuposNulos_noFalla() {
        VentaMapper mapper = new VentaMapper();
        DisponibilidadResponseDTO dto = mapper.toDisponibilidad(ID_EVENTO, null);

        assertNotNull(dto);
        assertEquals(0, dto.getDisponibleTotal());
        assertTrue(dto.isAgotado());
    }

    @Test
    void toReserva_mapeaCamposYDetalles() {
        VentaMapper mapper = new VentaMapper();
        Reserva r = reserva(ID_RESERVA, ID_CLIENTE, ID_EVENTO, "PENDIENTE",
                BigDecimal.valueOf(190000));
        List<DetalleReserva> detalles = new ArrayList<>();
        DetalleReserva d = new DetalleReserva();
        d.setIdDetalle("d1");
        d.setIdSector(ID_SECTOR);
        d.setCantidad(2);
        d.setPrecioUnitario(BigDecimal.valueOf(95000));
        detalles.add(d);

        ReservaResponseDTO dto = mapper.toReserva(r, detalles);

        assertNotNull(dto);
        assertEquals(ID_RESERVA, dto.getId());
        assertEquals(ID_CLIENTE, dto.getIdCliente());
        assertEquals(ID_EVENTO, dto.getIdEvento());
        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals(BigDecimal.valueOf(190000), dto.getTotal());
        assertEquals(1, dto.getDetalles().size());
        DetalleReservaDTO detDto = dto.getDetalles().get(0);
        assertEquals("d1", detDto.getIdDetalle());
        assertEquals(ID_SECTOR, detDto.getIdSector());
        assertEquals(Integer.valueOf(2), detDto.getCantidad());
        assertEquals(BigDecimal.valueOf(95000), detDto.getPrecioUnitario());
    }

    @Test
    void toReserva_detallesVacios_listaVacia() {
        VentaMapper mapper = new VentaMapper();
        Reserva r = reserva(ID_RESERVA, ID_CLIENTE, ID_EVENTO, "PENDIENTE", BigDecimal.TEN);

        ReservaResponseDTO dto = mapper.toReserva(r, new ArrayList<>());

        assertNotNull(dto);
        assertTrue(dto.getDetalles().isEmpty());
    }

    @Test
    void toCompra_conReserva() {
        VentaMapper mapper = new VentaMapper();
        Reserva reserva = reserva(ID_RESERVA, ID_CLIENTE, ID_EVENTO, "COMPRADA",
                BigDecimal.valueOf(190000));
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setReserva(reserva);
        compra.setIdCliente(ID_CLIENTE);
        compra.setFechaCompra(LocalDateTime.of(2026, 3, 1, 12, 5));
        compra.setTotal(BigDecimal.valueOf(190000));
        compra.setMoneda("ARS");
        compra.setEstado("APROBADA");
        compra.setEmisionEstado("EMITIDA");
        compra.setEmisionIntentos(1);

        CompraResponseDTO dto = mapper.toCompra(compra);

        assertNotNull(dto);
        assertEquals(ID_COMPRA, dto.getId());
        assertEquals(ID_RESERVA, dto.getIdReserva());
        assertEquals(ID_CLIENTE, dto.getIdCliente());
        assertEquals(BigDecimal.valueOf(190000), dto.getTotal());
        assertEquals("ARS", dto.getMoneda());
        assertEquals("APROBADA", dto.getEstado());
        assertEquals("EMITIDA", dto.getEmisionEstado());
        assertEquals(Integer.valueOf(1), dto.getEmisionIntentos());
    }

    @Test
    void toCompra_reservaNulo_idReservaNulo() {
        VentaMapper mapper = new VentaMapper();
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setReserva(null);
        compra.setIdCliente(ID_CLIENTE);
        compra.setFechaCompra(LocalDateTime.now());
        compra.setTotal(BigDecimal.TEN);
        compra.setMoneda("ARS");
        compra.setEstado("PENDIENTE_PAGO");
        compra.setEmisionEstado("NO_REQUIERE");
        compra.setEmisionIntentos(0);

        CompraResponseDTO dto = mapper.toCompra(compra);

        assertNotNull(dto);
        assertNull(dto.getIdReserva());
    }

    @Test
    void toPago_mapeaCampos() {
        VentaMapper mapper = new VentaMapper();
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        Pago pago = new Pago();
        pago.setIdPago("p1");
        pago.setCompra(compra);
        pago.setProveedor("MOCK_PASARELA");
        pago.setIdTransaccionExterna("mock-abc");
        pago.setMonto(BigDecimal.valueOf(190000));
        pago.setFechaPago(LocalDateTime.of(2026, 3, 1, 12, 6));
        pago.setEstado("APROBADO");

        PagoResponseDTO dto = mapper.toPago(pago);

        assertNotNull(dto);
        assertEquals("p1", dto.getId());
        assertEquals(ID_COMPRA, dto.getIdCompra());
        assertEquals("MOCK_PASARELA", dto.getProveedor());
        assertEquals("mock-abc", dto.getIdTransaccionExterna());
        assertEquals(BigDecimal.valueOf(190000), dto.getMonto());
        assertEquals("APROBADO", dto.getEstado());
    }
}
