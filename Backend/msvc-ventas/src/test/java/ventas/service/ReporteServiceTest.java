package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ventas.clients.EntradasClient;
import ventas.clients.EntradasClient.ResumenSector;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.ReporteDTO;
import ventas.entity.CupoSector;
import ventas.exception.ForbiddenException;
import ventas.exception.ResourceNotFoundException;
import ventas.repository.CompraRepository;
import ventas.repository.CupoSectorRepository;

/** E2-REP-01: cálculo y permisos de los reportes del organizador. */
class ReporteServiceTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String CAMPO = "c0000000-0000-4000-8000-000000000011";
    private static final String PLATEA = "c0000000-0000-4000-8000-000000000012";
    private static final String ORG = "b0000000-0000-4000-8000-000000000002";
    private static final String OTRO_ORG = "b0000000-0000-4000-8000-0000000000ff";

    private CompraRepository compraRepository;
    private CupoSectorRepository cupoRepository;
    private EventosClient eventosClient;
    private EntradasClient entradasClient;
    private ReporteServiceImpl service;

    @BeforeEach
    void setUp() {
        compraRepository = mock(CompraRepository.class);
        cupoRepository = mock(CupoSectorRepository.class);
        eventosClient = mock(EventosClient.class);
        entradasClient = mock(EntradasClient.class);
        service = new ReporteServiceImpl(compraRepository, cupoRepository, eventosClient, entradasClient);

        when(eventosClient.getEvento(EVENTO)).thenReturn(evento());
        when(cupoRepository.findByIdEvento(EVENTO)).thenReturn(List.of(cupo(CAMPO, 1000), cupo(PLATEA, 200)));
        when(compraRepository.contarPorEventoYEstado(EVENTO, "APROBADA")).thenReturn(7L);
        when(compraRepository.ventasPorSector(EVENTO, "APROBADA")).thenReturn(List.of(
                new Object[] {CAMPO, 50L, new BigDecimal("4750000.00")},
                new Object[] {PLATEA, 10L, new BigDecimal("1800000.00")}));
        when(entradasClient.resumenEvento(EVENTO)).thenReturn(List.of(
                new ResumenSector(CAMPO, 35, 10, 5, 0, new BigDecimal("475000.00"), BigDecimal.ZERO),
                new ResumenSector(PLATEA, 4, 2, 2, 2, new BigDecimal("360000.00"), new BigDecimal("360000.00"))));
    }

    private static EventoDetalleFeignDTO evento() {
        EventoDetalleFeignDTO e = new EventoDetalleFeignDTO();
        e.setId(EVENTO);
        e.setNombre("Metallica");
        e.setEstado("PUBLICADO");
        e.setIdOrganizador(ORG);
        e.setSectores(List.of(sector(CAMPO, "Campo"), sector(PLATEA, "Platea")));
        return e;
    }

    private static SectorFeignDTO sector(String id, String nombre) {
        SectorFeignDTO s = new SectorFeignDTO();
        s.setId(id);
        s.setNombre(nombre);
        s.setMoneda("ARS");
        return s;
    }

    private static CupoSector cupo(String idSector, int capacidad) {
        CupoSector c = new CupoSector();
        c.setIdEvento(EVENTO);
        c.setIdSector(idSector);
        c.setCapacidadTotal(capacidad);
        return c;
    }

    private static FeignException feign(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "http://x", Collections.emptyMap(), null,
                StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("m", feign.Response.builder().status(status).request(req)
                .headers(Collections.emptyMap()).build());
    }

    @Test
    void reporteCompleto_calculaVigentesOcupacionYRecaudacionNeta() {
        ReporteDTO.Evento r = service.reporte(EVENTO, ORG, "ORGANIZADOR");

        assertEquals("Metallica", r.nombre());
        assertEquals("ARS", r.moneda());
        assertEquals(7, r.compras());
        assertEquals(2, r.sectores().size());

        ReporteDTO.Sector campo = r.sectores().get(0);
        assertEquals("Campo", campo.nombre());
        assertEquals(50, campo.vendidas());
        assertEquals(5, campo.devueltas());
        assertEquals(45, campo.vigentes());                 // 50 - 5 - 0
        assertEquals(4.5, campo.ocupacionPct());            // 45 / 1000
        assertEquals(10, campo.ingresaron());
        assertEquals(0, new BigDecimal("4275000.00").compareTo(campo.recaudacionNeta())); // 4.750.000 - 475.000

        ReporteDTO.Sector platea = r.sectores().get(1);
        assertEquals(6, platea.vigentes());                 // 10 - 2 - 2
        assertEquals(3.0, platea.ocupacionPct());           // 6 / 200
        assertEquals(0, new BigDecimal("1080000.00").compareTo(platea.recaudacionNeta())); // 1.800.000 - 360.000 - 360.000

        ReporteDTO.Sector t = r.total();
        assertEquals("TOTAL", t.nombre());
        assertEquals(1200, t.capacidad());
        assertEquals(60, t.vendidas());
        assertEquals(51, t.vigentes());
        assertEquals(4.25, t.ocupacionPct());               // 51 / 1200
        assertEquals(0, new BigDecimal("6550000.00").compareTo(t.recaudacionBruta()));
        assertEquals(0, new BigDecimal("5355000.00").compareTo(t.recaudacionNeta()));
    }

    @Test
    void vistasDerivadas_coincidenConElReporteCompleto() {
        ReporteDTO.Ventas v = service.ventas(EVENTO, ORG, "ORGANIZADOR");
        assertEquals(7, v.compras());
        assertEquals(60, v.entradasVendidas());
        assertEquals(7, v.entradasDevueltas());
        assertEquals(2, v.entradasAReintegrar());
        assertEquals(51, v.entradasVigentes());
        assertEquals(12, v.ingresaron());

        ReporteDTO.Ocupacion o = service.ocupacion(EVENTO, ORG, "ORGANIZADOR");
        assertEquals(1200, o.capacidadTotal());
        assertEquals(51, o.vigentes());
        assertEquals(2, o.sectores().size());

        ReporteDTO.Recaudacion rec = service.recaudacion(EVENTO, ORG, "ORGANIZADOR");
        assertEquals("ARS", rec.moneda());
        assertEquals(0, new BigDecimal("6550000.00").compareTo(rec.bruta()));
        assertEquals(0, new BigDecimal("835000.00").compareTo(rec.devuelta()));
        assertEquals(0, new BigDecimal("360000.00").compareTo(rec.aReintegrar()));
        assertEquals(0, new BigDecimal("5355000.00").compareTo(rec.neta()));

        assertEquals(2, service.sectores(EVENTO, ORG, "ORGANIZADOR").size());
    }

    @Test
    void eventoSinVentas_devuelveCeros_sinDividirPorCero() {
        when(compraRepository.ventasPorSector(EVENTO, "APROBADA")).thenReturn(List.of());
        when(compraRepository.contarPorEventoYEstado(EVENTO, "APROBADA")).thenReturn(0L);
        when(entradasClient.resumenEvento(EVENTO)).thenReturn(List.of());
        when(cupoRepository.findByIdEvento(EVENTO)).thenReturn(List.of());

        ReporteDTO.Evento r = service.reporte(EVENTO, ORG, "ORGANIZADOR");

        assertEquals(2, r.sectores().size(), "salen los sectores del evento aunque no tengan ventas");
        assertEquals(0, r.total().vendidas());
        assertEquals(0.0, r.total().ocupacionPct());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.total().recaudacionNeta()));
    }

    @Test
    void vigentesNuncaSonNegativos() {
        when(entradasClient.resumenEvento(EVENTO)).thenReturn(List.of(
                new ResumenSector(CAMPO, 0, 0, 80, 0, BigDecimal.ZERO, BigDecimal.ZERO)));

        ReporteDTO.Sector campo = service.reporte(EVENTO, ORG, "ORGANIZADOR").sectores().get(0);

        assertEquals(0, campo.vigentes());
    }

    @Test
    void admin_veCualquierEvento() {
        assertEquals("Metallica", service.reporte(EVENTO, "cualquiera", "ADMIN").nombre());
    }

    @Test
    void organizadorAjeno_esForbidden_yNoConsultaVentasNiEntradas() {
        assertThrows(ForbiddenException.class, () -> service.reporte(EVENTO, OTRO_ORG, "ORGANIZADOR"));

        verify(compraRepository, never()).ventasPorSector(anyString(), anyString());
        verify(entradasClient, never()).resumenEvento(anyString());
    }

    @Test
    void otrosRoles_sonForbidden() {
        for (String rol : new String[] {"CLIENTE", "PERSONAL_ACCESO", null, ""}) {
            assertThrows(ForbiddenException.class, () -> service.reporte(EVENTO, ORG, rol), String.valueOf(rol));
        }
        verify(eventosClient, never()).getEvento(any());
    }

    @Test
    void eventoInexistente_es404() {
        when(eventosClient.getEvento(EVENTO)).thenThrow(feign(404));

        assertThrows(ResourceNotFoundException.class, () -> service.reporte(EVENTO, ORG, "ORGANIZADOR"));
    }

    @Test
    void siEventosOEntradasFallan_es503_yNoSeEntregaUnReporteParcial() {
        when(eventosClient.getEvento(EVENTO)).thenThrow(feign(500));
        ResponseStatusException e1 =
                assertThrows(ResponseStatusException.class, () -> service.reporte(EVENTO, ORG, "ORGANIZADOR"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e1.getStatusCode());

        doReturn(evento()).when(eventosClient).getEvento(EVENTO);
        when(entradasClient.resumenEvento(EVENTO)).thenThrow(feign(503));
        ResponseStatusException e2 =
                assertThrows(ResponseStatusException.class, () -> service.reporte(EVENTO, ORG, "ORGANIZADOR"));
        assertTrue(e2.getReason().contains("entradas"));
    }
}
