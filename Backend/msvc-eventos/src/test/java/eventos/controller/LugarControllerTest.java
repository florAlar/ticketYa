package eventos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eventos.exception.BusinessConflictException;
import eventos.exception.GlobalExceptionHandler;
import eventos.exception.ResourceNotFoundException;
import eventos.service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** LUG-01: baja de lugares (solo ADMIN; 409 si tiene eventos). */
class LugarControllerTest {

    private static final String LUGAR = "c0000000-0000-4000-8000-000000000001";

    private EventoService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(EventoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LugarController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void admin_eliminaElLugar_204() throws Exception {
        mvc.perform(delete("/lugares/{id}", LUGAR).header("X-User-Role", "ADMIN")).andExpect(status().isNoContent());

        verify(service).eliminarLugar(LUGAR);
    }

    @Test
    void otrosRolesOSinRol_403_yNoSeElimina() throws Exception {
        for (String rol : new String[] {"ORGANIZADOR", "CLIENTE", "PERSONAL_ACCESO", ""}) {
            mvc.perform(delete("/lugares/{id}", LUGAR).header("X-User-Role", rol)).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/lugares/{id}", LUGAR)).andExpect(status().isForbidden());

        verify(service, never()).eliminarLugar(any());
    }

    @Test
    void lugarConEventos_409() throws Exception {
        doThrow(new BusinessConflictException("El lugar tiene eventos asociados")).when(service).eliminarLugar(LUGAR);

        mvc.perform(delete("/lugares/{id}", LUGAR).header("X-User-Role", "ADMIN")).andExpect(status().isConflict());
    }

    @Test
    void altaYEdicionDeLugaresYSectores_soloAdmin_elOrganizadorRecibe403() throws Exception {
        String lugar = "{\"nombre\":\"Estadio\",\"ciudad\":\"Buenos Aires\"}";
        String sector = "{\"nombre\":\"Campo\",\"capacidad\":100}";
        for (String rol : new String[] {"ORGANIZADOR", "CLIENTE", "PERSONAL_ACCESO"}) {
            mvc.perform(post("/lugares").header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(lugar))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/lugares/{id}", LUGAR).header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(lugar))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/lugares/{id}/sectores", LUGAR).header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(sector))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/sectores/{id}", "s1").header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(sector))
                    .andExpect(status().isForbidden());
        }
        verify(service, never()).crearLugar(any());
        verify(service, never()).actualizarLugar(any(), any());
        verify(service, never()).crearSector(any(), any());
        verify(service, never()).actualizarSector(any(), any());
    }

    @Test
    void admin_puedeCrearYEditarLugaresYSectores() throws Exception {
        String lugar = "{\"nombre\":\"Estadio\",\"ciudad\":\"Buenos Aires\"}";
        String sector = "{\"nombre\":\"Campo\",\"capacidad\":100}";
        mvc.perform(post("/lugares").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON).content(lugar))
                .andExpect(status().isCreated());
        mvc.perform(put("/lugares/{id}", LUGAR).header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON).content(lugar))
                .andExpect(status().isOk());
        mvc.perform(post("/lugares/{id}/sectores", LUGAR).header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON).content(sector))
                .andExpect(status().isCreated());
        mvc.perform(put("/sectores/{id}", "s1").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON).content(sector))
                .andExpect(status().isOk());
    }

    @Test
    void lugarInexistente_404() throws Exception {
        doThrow(new ResourceNotFoundException("Lugar no encontrado")).when(service).eliminarLugar(LUGAR);

        mvc.perform(delete("/lugares/{id}", LUGAR).header("X-User-Role", "ADMIN")).andExpect(status().isNotFound());
    }
}
