package entradas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import entradas.clients.EventosClient;
import entradas.clients.VentasClient;
import entradas.dto.AsientoRefDTO;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.entity.Devolucion;
import entradas.entity.Entrada;
import entradas.mapper.EntradaMapper;
import entradas.repository.DevolucionRepository;
import entradas.repository.EntradaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Entradas con asiento virtual: emisión una por asiento y liberación del asiento al devolver. */
class EntradaServiceAsientosTest {

    private static final String ID_COMPRA = "c0000000-0000-4000-8000-000000000001";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String ID_EVENTO = "ev000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "s0000000-0000-4000-8000-000000000012";
    private static final String ID_DEVOLUCION = "d0000000-0000-4000-8000-000000000001";

    private EntradaRepository entradaRepository;
    private DevolucionRepository devolucionRepository;
    private VentasClient ventasClient;
    private EntradaServiceImpl service;

    @BeforeEach
    void setUp() {
        entradaRepository = mock(EntradaRepository.class);
        devolucionRepository = mock(DevolucionRepository.class);
        ventasClient = mock(VentasClient.class);
        service = new EntradaServiceImpl(
                entradaRepository, devolucionRepository, new EntradaMapper(),
                mock(EventosClient.class), ventasClient, 72L);
        when(entradaRepository.findByIdCompra(ID_COMPRA)).thenReturn(new ArrayList<>());
        when(entradaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private EmitirEntradasRequestDTO emision(Integer cantidad, AsientoRefDTO... asientos) {
        return new EmitirEntradasRequestDTO(
                ID_COMPRA, ID_CLIENTE, ID_EVENTO,
                List.of(new EmitirEntradasRequestDTO.Item(
                        ID_SECTOR, cantidad, BigDecimal.valueOf(180000),
                        asientos.length == 0 ? null : List.of(asientos))));
    }

    @Test
    void emitir_sectorNumerado_unaEntradaPorAsiento() {
        List<EntradaResponseDTO> res = service.emitir(
                emision(2, new AsientoRefDTO("C", 14), new AsientoRefDTO("C", 15)));

        assertEquals(2, res.size());
        assertEquals("C", res.get(0).getFila());
        assertEquals(14, res.get(0).getAsiento());
        assertEquals("C", res.get(1).getFila());
        assertEquals(15, res.get(1).getAsiento());
        assertEquals(ID_SECTOR, res.get(0).getIdSector());
        assertEquals(BigDecimal.valueOf(180000), res.get(0).getPrecio());
    }

    @Test
    void emitir_sectorNumerado_codigosQrDistintos() {
        List<EntradaResponseDTO> res = service.emitir(
                emision(2, new AsientoRefDTO("A", 1), new AsientoRefDTO("A", 2)));

        assertEquals(2, res.size());
        assertEquals(false, res.get(0).getCodigoQR().equals(res.get(1).getCodigoQR()));
    }

    @Test
    void emitir_cantidadNoCoincideConAsientos_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.emitir(emision(3, new AsientoRefDTO("A", 1), new AsientoRefDTO("A", 2))));
    }

    @Test
    void emitir_sectorGeneral_sinAsiento() {
        List<EntradaResponseDTO> res = service.emitir(emision(3));

        assertEquals(3, res.size());
        assertNull(res.get(0).getFila());
        assertNull(res.get(0).getAsiento());
    }

    // ---------- devolución ----------

    private Devolucion devolucionPendiente(String fila, Integer numero) {
        Entrada e = new Entrada();
        e.setIdEntrada("ent-1");
        e.setIdCompra(ID_COMPRA);
        e.setIdCliente(ID_CLIENTE);
        e.setIdEvento(ID_EVENTO);
        e.setIdSector(ID_SECTOR);
        e.setFila(fila);
        e.setNumero(numero);
        e.setEstado("VENDIDA");
        e.setPrecio(BigDecimal.valueOf(180000));
        e.setFechaEmision(LocalDateTime.of(2026, 3, 1, 12, 0));
        e.setCodigoQr("QR-1");
        Devolucion d = new Devolucion();
        d.setIdDevolucion(ID_DEVOLUCION);
        d.setEntrada(e);
        d.setEstado("PENDIENTE");
        when(devolucionRepository.findByIdWithEntrada(ID_DEVOLUCION)).thenReturn(Optional.of(d));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        return d;
    }

    @Test
    void aprobarDevolucion_entradaConAsiento_liberaEseAsiento() {
        devolucionPendiente("C", 14);

        service.aprobarDevolucion(ID_DEVOLUCION);

        ArgumentCaptor<VentasClient.LiberarCupoBody> captor =
                ArgumentCaptor.forClass(VentasClient.LiberarCupoBody.class);
        verify(ventasClient).liberarCupo(org.mockito.ArgumentMatchers.eq(ID_EVENTO), captor.capture());
        VentasClient.LiberarCupoBody body = captor.getValue();
        assertEquals(ID_SECTOR, body.getIdSector());
        assertEquals(1, body.getCantidad());
        assertEquals(1, body.getAsientos().size());
        assertEquals("C", body.getAsientos().get(0).fila());
        assertEquals(14, body.getAsientos().get(0).numero());
    }

    @Test
    void aprobarDevolucion_entradaGeneral_liberaPorCantidad() {
        devolucionPendiente(null, null);

        service.aprobarDevolucion(ID_DEVOLUCION);

        ArgumentCaptor<VentasClient.LiberarCupoBody> captor =
                ArgumentCaptor.forClass(VentasClient.LiberarCupoBody.class);
        verify(ventasClient).liberarCupo(anyString(), captor.capture());
        assertEquals(1, captor.getValue().getCantidad());
        assertNull(captor.getValue().getAsientos());
    }
}
