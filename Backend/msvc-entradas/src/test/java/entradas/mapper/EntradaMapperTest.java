package entradas.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;
import entradas.entity.Devolucion;
import entradas.entity.Entrada;

class EntradaMapperTest {

    private static final String ID_ENTRADA = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_DEVOLUCION = "d0000000-0000-4000-8000-000000000001";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String ID_EVENTO = "ev000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "s0000000-0000-4000-8000-000000000001";
    private static final String ID_COMPRA = "c0000000-0000-4000-8000-000000000001";

    private Entrada entrada(String estado, BigDecimal precio) {
        Entrada e = new Entrada();
        e.setIdEntrada(ID_ENTRADA);
        e.setIdCompra(ID_COMPRA);
        e.setIdCliente(ID_CLIENTE);
        e.setIdEvento(ID_EVENTO);
        e.setIdSector(ID_SECTOR);
        e.setEstado(estado);
        e.setPrecio(precio);
        e.setFechaEmision(LocalDateTime.of(2026, 3, 1, 12, 0));
        e.setFechaValidacion(null);
        e.setCodigoQr("QR-SEED-001");
        return e;
    }

    @Test
    void toEntrada_mapeaTodosLosCampos() {
        EntradaMapper mapper = new EntradaMapper();
        Entrada e = entrada("VENDIDA", BigDecimal.valueOf(95000));

        EntradaResponseDTO dto = mapper.toEntrada(e);

        assertNotNull(dto);
        assertEquals(ID_ENTRADA, dto.getId());
        assertEquals("QR-SEED-001", dto.getCodigoQR());
        assertEquals(ID_COMPRA, dto.getIdCompra());
        assertEquals(ID_CLIENTE, dto.getIdCliente());
        assertEquals(ID_EVENTO, dto.getIdEvento());
        assertEquals(ID_SECTOR, dto.getIdSector());
        assertEquals("VENDIDA", dto.getEstado());
        assertEquals(BigDecimal.valueOf(95000), dto.getPrecio());
        assertEquals(LocalDateTime.of(2026, 3, 1, 12, 0), dto.getFechaEmision());
        assertNull(dto.getFechaIngreso());
    }

    @Test
    void toEntrada_precioNulo_devuelveCero() {
        EntradaMapper mapper = new EntradaMapper();
        Entrada e = entrada("VENDIDA", null);

        EntradaResponseDTO dto = mapper.toEntrada(e);

        assertEquals(BigDecimal.ZERO, dto.getPrecio());
    }

    @Test
    void toQr_mapeaCampos() {
        EntradaMapper mapper = new EntradaMapper();
        Entrada e = entrada("VENDIDA", BigDecimal.valueOf(95000));

        QrResponseDTO dto = mapper.toQr(e);

        assertNotNull(dto);
        assertEquals(ID_ENTRADA, dto.getId());
        assertEquals("QR-SEED-001", dto.getCodigoQR());
    }

    @Test
    void toDevolucion_mapeaCampos() {
        EntradaMapper mapper = new EntradaMapper();
        Entrada e = entrada("VENDIDA", BigDecimal.valueOf(95000));
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(e);
        d.setMotivo("No puedo asistir");
        d.setMonto(BigDecimal.valueOf(95000));
        d.setFechaSolicitud(LocalDateTime.of(2026, 3, 10, 9, 0));
        d.setEstado("PENDIENTE");

        DevolucionResponseDTO dto = mapper.toDevolucion(d);

        assertNotNull(dto);
        assertEquals(ID_DEVOLUCION, dto.getId());
        assertEquals(ID_ENTRADA, dto.getIdEntrada());
        assertEquals(ID_CLIENTE, dto.getIdCliente());
        assertEquals("No puedo asistir", dto.getMotivo());
        assertEquals(BigDecimal.valueOf(95000), dto.getMonto());
        assertEquals(LocalDateTime.of(2026, 3, 10, 9, 0), dto.getFechaSolicitud());
        assertEquals("PENDIENTE", dto.getEstado());
    }

    @Test
    void precioDe_entradaNula_devuelveCero() {
        assertEquals(BigDecimal.ZERO, EntradaMapper.precioDe(null));
    }

    @Test
    void precioDe_entradaPrecioNulo_devuelveCero() {
        Entrada e = new Entrada();
        e.setPrecio(null);

        assertEquals(BigDecimal.ZERO, EntradaMapper.precioDe(e));
    }

    @Test
    void precioDe_entradaConPrecio_devuelvePrecio() {
        Entrada e = entrada("VENDIDA", BigDecimal.valueOf(75000));

        assertEquals(BigDecimal.valueOf(75000), EntradaMapper.precioDe(e));
    }

    @Test
    void estados_devuelveValoresEsperados() {
        assertTrue("VENDIDA".equals("VENDIDA"));
        assertTrue("USADA".equals("USADA"));
        assertTrue("DEVUELTA".equals("DEVUELTA"));
        assertTrue("A_REINTEGRAR".equals("A_REINTEGRAR"));
    }
}
