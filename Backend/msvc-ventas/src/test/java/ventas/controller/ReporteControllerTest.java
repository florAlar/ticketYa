package ventas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ventas.dto.ReporteDTO;
import ventas.exception.ForbiddenException;
import ventas.exception.GlobalExceptionHandler;
import ventas.exception.ResourceNotFoundException;
import ventas.service.ReporteService;

/** E2-REP-01: contrato HTTP de /reportes/eventos/{id}/** (identidad = headers del gateway). */
class ReporteControllerTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String ORG = "b0000000-0000-4000-8000-000000000002";

    private ReporteService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ReporteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReporteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ReporteDTO.Sector fila() {
        return new ReporteDTO.Sector(null, "TOTAL", 1200, 60, 7, 2, 51, 12, 4.25,
                new BigDecimal("6550000.00"), new BigDecimal("835000.00"), new BigDecimal("360000.00"),
                new BigDecimal("5355000.00"));
    }

    private org.springframework.test.web.servlet.ResultActions pedir(String path, String role) throws Exception {
        var req = get("/reportes/eventos/{id}" + path, EVENTO).header("X-User-Id", ORG);
        if (role != null) req.header("X-User-Role", role);
        return mvc.perform(req);
    }

    @Test
    void reporteCompleto_ok() throws Exception {
        when(service.reporte(EVENTO, ORG, "ORGANIZADOR")).thenReturn(new ReporteDTO.Evento(
                EVENTO, "Metallica", "PUBLICADO", "ARS", LocalDateTime.of(2027, 1, 1, 12, 0), 7, fila(), List.of()));

        pedir("", "ORGANIZADOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Metallica"))
                .andExpect(jsonPath("$.total.vigentes").value(51))
                .andExpect(jsonPath("$.total.ocupacionPct").value(4.25));
    }

    @Test
    void ventas_sectores_ocupacion_recaudacion_ok() throws Exception {
        when(service.ventas(EVENTO, ORG, "ADMIN"))
                .thenReturn(new ReporteDTO.Ventas(EVENTO, 7, 60, 7, 2, 51, 12));
        when(service.sectores(EVENTO, ORG, "ADMIN")).thenReturn(List.of(fila()));
        when(service.ocupacion(EVENTO, ORG, "ADMIN"))
                .thenReturn(new ReporteDTO.Ocupacion(EVENTO, 1200, 51, 4.25, List.of()));
        when(service.recaudacion(EVENTO, ORG, "ADMIN")).thenReturn(new ReporteDTO.Recaudacion(
                EVENTO, "ARS", new BigDecimal("6550000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("6550000.00"), List.of()));

        pedir("/ventas", "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$.entradasVigentes").value(51));
        pedir("/sectores", "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("TOTAL"));
        pedir("/ocupacion", "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$.capacidadTotal").value(1200));
        pedir("/recaudacion", "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$.moneda").value("ARS"));
    }

    @Test
    void sinIdentidad_401() throws Exception {
        mvc.perform(get("/reportes/eventos/{id}", EVENTO).header("X-User-Role", "ORGANIZADOR"))
                .andExpect(status().isUnauthorized());

        verify(service, never()).reporte(any(), any(), any());
    }

    @Test
    void rolSinPermiso_o_organizadorAjeno_403() throws Exception {
        when(service.reporte(eq(EVENTO), eq(ORG), eq("CLIENTE")))
                .thenThrow(new ForbiddenException("Los reportes son para el organizador"));
        when(service.reporte(eq(EVENTO), eq(ORG), eq("ORGANIZADOR")))
                .thenThrow(new ForbiddenException("Solo el organizador del evento"));

        pedir("", "CLIENTE").andExpect(status().isForbidden());
        pedir("", "ORGANIZADOR").andExpect(status().isForbidden());
    }

    @Test
    void eventoInexistente_404() throws Exception {
        when(service.reporte(eq(EVENTO), eq(ORG), eq("ORGANIZADOR")))
                .thenThrow(new ResourceNotFoundException("Evento no encontrado"));

        pedir("", "ORGANIZADOR").andExpect(status().isNotFound());
    }
}
