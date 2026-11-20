package ventas.service;

import ventas.dto.FilaEstadoResponseDTO;
import ventas.dto.FilaHabilitacionResponseDTO;
import ventas.dto.FilaIngresoResponseDTO;
import ventas.dto.FilaPosicionResponseDTO;
import ventas.dto.FilaValidacionResponseDTO;

/** Contrato de la fila virtual usado por el controller y por el gate de reservas (FIL-03). */
public interface FilaService {

    record IngresoResultado(FilaIngresoResponseDTO respuesta, boolean creado) {}

    IngresoResultado ingresar(String idEvento, String userId);

    FilaPosicionResponseDTO posicion(String idEvento, String userId);

    FilaEstadoResponseDTO estado(String idEvento);

    /** Override ADMIN de la tanda (FIL-02). Rol distinto de ADMIN → 403. */
    FilaHabilitacionResponseDTO habilitar(String idEvento, Integer hastaPosicion, String rol);

    /** Emite o devuelve el token vigente de un cliente habilitado (FIL-03). */
    FilaValidacionResponseDTO validarToken(String idEvento, String userId);

    /** Consume el token de forma atómica; false si es inexistente, vencido, usado o ajeno. */
    boolean consumirToken(String idEvento, String userId, String token);

    /** Restaura el token cuando la transacción de la reserva no llega a commit. */
    void restaurarToken(String idEvento, String userId, String token);

    /** Descarta el flag/estado cacheado tras publicar, suspender o cambiar alta demanda. */
    void invalidarCache(String idEvento);
}
