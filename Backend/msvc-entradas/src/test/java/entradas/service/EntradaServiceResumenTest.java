package entradas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import entradas.clients.EventosClient;
import entradas.clients.VentasClient;
import entradas.dto.ResumenSectorDTO;
import entradas.mapper.EntradaMapper;
import entradas.repository.DevolucionRepository;
import entradas.repository.EntradaRepository;
import entradas.repository.ValidacionAccesoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reportes: resumen interno de entradas por sector y estado. */
class EntradaServiceResumenTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String CAMPO = "c0000000-0000-4000-8000-000000000011";
    private static final String PLATEA = "c0000000-0000-4000-8000-000000000012";

    private EntradaRepository repo;
    private EntradaServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(EntradaRepository.class);
        service = new EntradaServiceImpl(repo, mock(DevolucionRepository.class), new EntradaMapper(),
                mock(EventosClient.class), mock(VentasClient.class), mock(ValidacionAccesoRepository.class), 72L);
    }

    @Test
    void agrupaPorSector_acumulaCantidadesYMontosPorEstado() {
        when(repo.resumenPorSectorYEstado(EVENTO)).thenReturn(List.of(
                new Object[] {CAMPO, "VENDIDA", 10L, new BigDecimal("950000.00")},
                new Object[] {CAMPO, "USADA", 4L, new BigDecimal("380000.00")},
                new Object[] {CAMPO, "DEVUELTA", 2L, new BigDecimal("190000.00")},
                new Object[] {PLATEA, "VENDIDA", 3L, new BigDecimal("540000.00")},
                new Object[] {PLATEA, "A_REINTEGRAR", 5L, new BigDecimal("900000.00")}));

        List<ResumenSectorDTO> out = service.resumenPorEvento(EVENTO);

        assertEquals(2, out.size());
        ResumenSectorDTO campo = out.stream().filter(r -> r.idSector().equals(CAMPO)).findFirst().orElseThrow();
        assertEquals(10, campo.vendidas());
        assertEquals(4, campo.usadas());
        assertEquals(2, campo.devueltas());
        assertEquals(0, campo.aReintegrar());
        assertEquals(0, new BigDecimal("190000.00").compareTo(campo.montoDevuelto()));
        assertEquals(0, BigDecimal.ZERO.compareTo(campo.montoAReintegrar()));

        ResumenSectorDTO platea = out.stream().filter(r -> r.idSector().equals(PLATEA)).findFirst().orElseThrow();
        assertEquals(3, platea.vendidas());
        assertEquals(5, platea.aReintegrar());
        assertEquals(0, new BigDecimal("900000.00").compareTo(platea.montoAReintegrar()));
    }

    @Test
    void eventoSinEntradas_devuelveVacio() {
        when(repo.resumenPorSectorYEstado(EVENTO)).thenReturn(List.of());

        assertTrue(service.resumenPorEvento(EVENTO).isEmpty());
    }

    @Test
    void tolera_sumasNumericasNoBigDecimal_yEstadosDesconocidos() {
        when(repo.resumenPorSectorYEstado(EVENTO)).thenReturn(List.of(
                new Object[] {CAMPO, "DEVUELTA", 1L, 95000.0},
                new Object[] {CAMPO, "OTRO_ESTADO", 9L, 1.0}));

        ResumenSectorDTO r = service.resumenPorEvento(EVENTO).get(0);

        assertEquals(1, r.devueltas());
        assertEquals(0, new BigDecimal("95000.0").compareTo(r.montoDevuelto()));
        assertEquals(0, r.vendidas());
    }
}
