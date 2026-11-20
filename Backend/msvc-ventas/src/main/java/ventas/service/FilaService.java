package ventas.service;

import ventas.dto.FilaEstadoResponseDTO;
import ventas.dto.FilaHabilitacionResponseDTO;
import ventas.dto.FilaIngresoResponseDTO;
import ventas.dto.FilaPosicionResponseDTO;

/** Contrato de la fila virtual usado por el controller y por el gate de reservas (FIL-03). */
public interface FilaService {

    record IngresoResultado(FilaIngresoResponseDTO respuesta, boolean creado) {}

    IngresoResultado ingresar(String idEvento, String userId);

    FilaPosicionResponseDTO posicion(String idEvento, String userId);

    FilaEstadoResponseDTO estado(String idEvento);

    /** Override ADMIN de la tanda (FIL-02). Rol distinto de ADMIN → 403. */
    FilaHabilitacionResponseDTO habilitar(String idEvento, Integer hastaPosicion, String rol);
}
