package eventos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eventos.dto.ParticipanteResponseDTO;
import eventos.exception.BusinessConflictException;
import eventos.exception.GlobalExceptionHandler;
import eventos.exception.ResourceNotFoundException;
import eventos.service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** PAR-01: el organizador da de alta participantes; editar y eliminar es solo ADMIN. */
class ParticipanteControllerTest {

    private static final String ID = "p0000000-0000-4000-8000-000000000001";
    private static final String BODY = "{\"nombre\":\"Metallica\",\"tipo\":\"BANDA\"}";

    private EventoService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(EventoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ParticipanteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void organizadorYAdmin_puedenDarDeAlta_201() throws Exception {
        when(service.crearParticipante(any())).thenReturn(new ParticipanteResponseDTO());

        for (String rol : new String[] {"ORGANIZADOR", "ADMIN"}) {
            mvc.perform(post("/participantes").header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void clienteOSinRol_noPuedeDarDeAlta_403() throws Exception {
        for (String rol : new String[] {"CLIENTE", "PERSONAL_ACCESO", ""}) {
            mvc.perform(post("/participantes").header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/participantes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(service, never()).crearParticipante(any());
    }

    @Test
    void duplicado_409() throws Exception {
        when(service.crearParticipante(any())).thenThrow(new BusinessConflictException("Ya existe un participante"));

        mvc.perform(post("/participantes").header("X-User-Role", "ORGANIZADOR").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void editar_soloAdmin() throws Exception {
        when(service.actualizarParticipante(any(), any())).thenReturn(new ParticipanteResponseDTO());

        mvc.perform(put("/participantes/{id}", ID).header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        for (String rol : new String[] {"ORGANIZADOR", "CLIENTE", "PERSONAL_ACCESO"}) {
            mvc.perform(put("/participantes/{id}", ID).header("X-User-Role", rol).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
        verify(service).actualizarParticipante(any(), any());
    }

    @Test
    void eliminar_soloAdmin_204() throws Exception {
        mvc.perform(delete("/participantes/{id}", ID).header("X-User-Role", "ADMIN")).andExpect(status().isNoContent());
        verify(service).eliminarParticipante(ID);

        for (String rol : new String[] {"ORGANIZADOR", "CLIENTE", ""}) {
            mvc.perform(delete("/participantes/{id}", ID).header("X-User-Role", rol)).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/participantes/{id}", ID)).andExpect(status().isForbidden());
    }

    @Test
    void eliminar_enUso_409_inexistente_404() throws Exception {
        doThrow(new BusinessConflictException("participa en eventos")).when(service).eliminarParticipante(ID);
        mvc.perform(delete("/participantes/{id}", ID).header("X-User-Role", "ADMIN")).andExpect(status().isConflict());

        doThrow(new ResourceNotFoundException("no existe")).when(service).eliminarParticipante("nope");
        mvc.perform(delete("/participantes/{id}", "nope").header("X-User-Role", "ADMIN")).andExpect(status().isNotFound());
    }
}
