package preferencias.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import preferencias.dto.HistorialItemDTO;
import preferencias.dto.PageResponseDTO;
import preferencias.exception.ForbiddenException;
import preferencias.exception.UnauthorizedException;
import preferencias.service.InteraccionService;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private InteraccionService interaccionService;

    @Test
    void duenioPuedeVerSuHistorial() {
        ClienteController controller = new ClienteController(interaccionService);
        PageResponseDTO<HistorialItemDTO> page =
                new PageResponseDTO<>(List.of(), 0, 20, 0, 0);
        when(interaccionService.historial("cli-1", 0, 20)).thenReturn(page);

        assertEquals(page, controller.historial("cli-1", "cli-1", "CLIENTE", 0, 20));
    }

    @Test
    void adminPuedeVerHistorialDeOtro() {
        ClienteController controller = new ClienteController(interaccionService);
        PageResponseDTO<HistorialItemDTO> page =
                new PageResponseDTO<>(List.of(), 0, 20, 0, 0);
        when(interaccionService.historial("cli-2", 0, 20)).thenReturn(page);

        assertEquals(page, controller.historial("cli-2", "admin-1", "CLIENTE,ADMIN", 0, 20));
    }

    @Test
    void otroClienteNoPuedeVerElHistorial() {
        ClienteController controller = new ClienteController(interaccionService);

        assertThrows(ForbiddenException.class,
                () -> controller.historial("cli-2", "cli-1", "CLIENTE", 0, 20));
    }

    @Test
    void sinIdentidadDevuelve401() {
        ClienteController controller = new ClienteController(interaccionService);

        assertThrows(UnauthorizedException.class,
                () -> controller.historial("cli-1", null, "CLIENTE", 0, 20));
    }
}
