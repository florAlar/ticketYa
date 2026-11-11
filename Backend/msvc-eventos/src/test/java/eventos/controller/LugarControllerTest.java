package eventos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eventos.exception.BusinessConflictException;
import eventos.exception.GlobalExceptionHandler;
import eventos.exception.ResourceNotFoundException;
import eventos.service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    void lugarInexistente_404() throws Exception {
        doThrow(new ResourceNotFoundException("Lugar no encontrado")).when(service).eliminarLugar(LUGAR);

        mvc.perform(delete("/lugares/{id}", LUGAR).header("X-User-Role", "ADMIN")).andExpect(status().isNotFound());
    }
}
