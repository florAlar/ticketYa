package ventas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ventas.dto.FilaEstadoResponseDTO;
import ventas.dto.FilaHabilitacionResponseDTO;
import ventas.dto.FilaIngresoResponseDTO;
import ventas.dto.FilaPosicionResponseDTO;
import ventas.exception.FilaException;
import ventas.exception.GlobalExceptionHandler;
import ventas.service.FilaService;

/** FIL-01: contrato HTTP de los tres endpoints implementados en este bloque. */
class FilaControllerTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String USER = "b0000000-0000-4000-8000-000000000001";

    private FilaService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(FilaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FilaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static FilaIngresoResponseDTO respuesta() {
        return new FilaIngresoResponseDTO(EVENTO, 57, 50, 7, "EN_ESPERA");
    }

    @Test
    void ingresoPrimeraVez_201() throws Exception {
        when(service.ingresar(EVENTO, USER)).thenReturn(new FilaService.IngresoResultado(respuesta(), true));

        mvc.perform(post("/eventos/{id}/fila/ingreso", EVENTO).header("X-User-Id", USER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.posicion").value(57))
                .andExpect(jsonPath("$.delante").value(7))
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"));
    }

    @Test
    void reingreso_200() throws Exception {
        when(service.ingresar(EVENTO, USER)).thenReturn(new FilaService.IngresoResultado(respuesta(), false));

        mvc.perform(post("/eventos/{id}/fila/ingreso", EVENTO).header("X-User-Id", USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicion").value(57));
    }

    @Test
    void ingresoSinIdentidad_401() throws Exception {
        mvc.perform(post("/eventos/{id}/fila/ingreso", EVENTO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eventoNoElegible_409ConCode() throws Exception {
        when(service.ingresar(any(), any())).thenThrow(FilaException.eventoNoElegible());

        mvc.perform(post("/eventos/{id}/fila/ingreso", EVENTO).header("X-User-Id", USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FILA_EVENTO_NO_ELEGIBLE"));
    }

    @Test
    void redisCaido_503ConCode() throws Exception {
        when(service.posicion(any(), any())).thenThrow(FilaException.noDisponible());

        mvc.perform(get("/eventos/{id}/fila/posicion", EVENTO).header("X-User-Id", USER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FILA_NO_DISPONIBLE"));
    }

    @Test
    void posicion_200() throws Exception {
        when(service.posicion(EVENTO, USER))
                .thenReturn(new FilaPosicionResponseDTO(EVENTO, 40, 100, 0, "HABILITADO"));

        mvc.perform(get("/eventos/{id}/fila/posicion", EVENTO).header("X-User-Id", USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("HABILITADO"))
                .andExpect(jsonPath("$.delante").value(0));
    }

    @Test
    void habilitacion_admin_200() throws Exception {
        when(service.habilitar(EVENTO, 100, "ADMIN"))
                .thenReturn(new FilaHabilitacionResponseDTO(EVENTO, 100, "MANUAL"));

        mvc.perform(post("/eventos/{id}/fila/habilitacion", EVENTO)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hastaPosicion\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicionHabilitadaHasta").value(100))
                .andExpect(jsonPath("$.modo").value("MANUAL"));
    }

    @Test
    void habilitacion_soloAdminInclusoSiElGatewayDejaPasar() throws Exception {
        for (String rol : new String[] {"CLIENTE", "ORGANIZADOR", "PERSONAL_ACCESO"}) {
            mvc.perform(post("/eventos/{id}/fila/habilitacion", EVENTO)
                            .header("X-User-Role", rol)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hastaPosicion\":100}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/eventos/{id}/fila/habilitacion", EVENTO)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hastaPosicion\":100}"))
                .andExpect(status().isOk());
    }

    @Test
    void habilitacion_hastaPosicionInvalido_400() throws Exception {
        for (String body : new String[] {"{}", "{\"hastaPosicion\":0}", "{\"hastaPosicion\":-1}"}) {
            mvc.perform(post("/eventos/{id}/fila/habilitacion", EVENTO)
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void habilitacion_tandaNoReducible_409ConCode() throws Exception {
        when(service.habilitar(any(), any(), any())).thenThrow(FilaException.tandaNoReducible());

        mvc.perform(post("/eventos/{id}/fila/habilitacion", EVENTO)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hastaPosicion\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FILA_TANDA_NO_REDUCIBLE"));
    }

    @Test
    void habilitacion_redisCaido_503ConCode() throws Exception {
        when(service.habilitar(any(), any(), any())).thenThrow(FilaException.noDisponible());

        mvc.perform(post("/eventos/{id}/fila/habilitacion", EVENTO)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hastaPosicion\":100}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FILA_NO_DISPONIBLE"));
    }

    @Test
    void estado_publicoSinHeader() throws Exception {
        when(service.estado(EVENTO)).thenReturn(new FilaEstadoResponseDTO(EVENTO, 340, 100, true));

        mvc.perform(get("/eventos/{id}/fila/estado", EVENTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnFila").value(340))
                .andExpect(jsonPath("$.abierta").value(true));
    }
}
