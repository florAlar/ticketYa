package entradas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import entradas.dto.ValidacionResponseDTO;
import entradas.exception.ForbiddenException;
import entradas.exception.GlobalExceptionHandler;
import entradas.exception.ResourceNotFoundException;
import entradas.service.EntradaService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** E2-ACC-01: contrato HTTP y roles de /eventos/{id}/validaciones (identidad = headers del gateway). */
class EntradaControllerValidacionTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String USER = "b0000000-0000-4000-8000-000000000004";
    private static final String BODY = "{\"codigoQR\":\"QR-8F3A1C5D2B7E9A40\",\"dispositivo\":\"puerta-3\"}";

    private EntradaService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(EntradaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EntradaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ValidacionResponseDTO respuesta(boolean ok, String resultado) {
        ValidacionResponseDTO r = new ValidacionResponseDTO();
        r.setOk(ok);
        r.setResultado(resultado);
        r.setMensaje(resultado);
        return r;
    }

    private org.springframework.test.web.servlet.ResultActions postValidar(String role, String body) throws Exception {
        var req = post("/eventos/{id}/validaciones", EVENTO).contentType(MediaType.APPLICATION_JSON).content(body);
        if (role != null) req.header("X-User-Role", role);
        req.header("X-User-Id", USER);
        return mvc.perform(req);
    }

    @Test
    void personalAcceso_validaOk_devuelve200() throws Exception {
        when(service.validarIngreso(eq(EVENTO), eq(USER), any())).thenReturn(respuesta(true, "VALIDA"));

        postValidar("PERSONAL_ACCESO", BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.resultado").value("VALIDA"));
    }

    @Test
    void admin_tambienPuedeValidar() throws Exception {
        when(service.validarIngreso(eq(EVENTO), eq(USER), any())).thenReturn(respuesta(true, "VALIDA"));

        postValidar("ADMIN", BODY).andExpect(status().isOk());
    }

    @Test
    void ingresoRechazado_devuelve409_conElResultadoEnElBody() throws Exception {
        for (String r : List.of("YA_USADA", "OTRO_EVENTO", "NO_VALIDA", "EVENTO_CANCELADO")) {
            when(service.validarIngreso(eq(EVENTO), eq(USER), any())).thenReturn(respuesta(false, r));

            postValidar("PERSONAL_ACCESO", BODY)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.resultado").value(r));
        }
    }

    @Test
    void clienteYOrganizador_noPuedenValidar_403() throws Exception {
        for (String rol : List.of("CLIENTE", "ORGANIZADOR")) {
            postValidar(rol, BODY).andExpect(status().isForbidden());
        }
        postValidar(null, BODY).andExpect(status().isForbidden());
        verify(service, never()).validarIngreso(any(), any(), any());
    }

    @Test
    void sinIdentidad_401() throws Exception {
        mvc.perform(post("/eventos/{id}/validaciones", EVENTO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .header("X-User-Role", "PERSONAL_ACCESO"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void qrVacioOMalformado_400() throws Exception {
        postValidar("PERSONAL_ACCESO", "{\"codigoQR\":\"  \"}").andExpect(status().isBadRequest());
        postValidar("PERSONAL_ACCESO", "{}").andExpect(status().isBadRequest());
        verify(service, never()).validarIngreso(any(), any(), any());
    }

    @Test
    void qrInexistente_404() throws Exception {
        when(service.validarIngreso(eq(EVENTO), eq(USER), any()))
                .thenThrow(new ResourceNotFoundException("Entrada no encontrada para el QR"));

        postValidar("PERSONAL_ACCESO", BODY).andExpect(status().isNotFound());
    }

    @Test
    void listado_soloAdminYOrganizador() throws Exception {
        when(service.listarValidaciones(EVENTO, USER, true)).thenReturn(List.of());
        when(service.listarValidaciones(EVENTO, USER, false)).thenReturn(List.of());

        mvc.perform(get("/eventos/{id}/validaciones", EVENTO).header("X-User-Id", USER).header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
        mvc.perform(get("/eventos/{id}/validaciones", EVENTO).header("X-User-Id", USER).header("X-User-Role", "ORGANIZADOR"))
                .andExpect(status().isOk());
        mvc.perform(get("/eventos/{id}/validaciones", EVENTO).header("X-User-Id", USER).header("X-User-Role", "PERSONAL_ACCESO"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/eventos/{id}/validaciones", EVENTO).header("X-User-Id", USER).header("X-User-Role", "CLIENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listado_organizadorAjeno_403() throws Exception {
        when(service.listarValidaciones(EVENTO, USER, false))
                .thenThrow(new ForbiddenException("Solo el organizador del evento o un ADMIN"));

        mvc.perform(get("/eventos/{id}/validaciones", EVENTO).header("X-User-Id", USER).header("X-User-Role", "ORGANIZADOR"))
                .andExpect(status().isForbidden());
    }
}
