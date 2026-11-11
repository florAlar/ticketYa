package entradas.service;

import java.util.List;
import entradas.dto.DevolucionCreateRequestDTO;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;
import entradas.dto.ValidacionRegistroDTO;
import entradas.dto.ValidacionRequestDTO;
import entradas.dto.ValidacionResponseDTO;

public interface EntradaService {

    List<EntradaResponseDTO> emitir(EmitirEntradasRequestDTO request);

    List<EntradaResponseDTO> listarMias(String idCliente);

    /**
     * Detalle por id: dueño (idCliente) o ADMIN (cualquier entrada).
     * Un solo endpoint GET /entradas/{id}.
     */
    EntradaResponseDTO obtener(String idCliente, String idEntrada, boolean admin);

    QrResponseDTO qrMia(String idCliente, String idEntrada);

    DevolucionResponseDTO solicitarDevolucion(
            String idCliente, String idEntrada, DevolucionCreateRequestDTO body);

    DevolucionResponseDTO obtenerDevolucion(String idCliente, String idDevolucion, boolean admin);

    List<DevolucionResponseDTO> listarDevolucionesPendientes();

    List<DevolucionResponseDTO> listarDevolucionesCliente(String idCliente);

    DevolucionResponseDTO aprobarDevolucion(String idDevolucion);

    DevolucionResponseDTO rechazarDevolucion(String idDevolucion);

    /** E1-ENT-05: marca entradas VENDIDA del evento como A_REINTEGRAR. */
    int reintegroPorCancelacionEvento(String idEvento);

    /**
     * E2-ACC-01: valida una entrada en la puerta. Verifica que exista, corresponda al evento y no se
     * haya usado; si es válida la marca USADA. Todo intento sobre una entrada existente queda
     * registrado en validacion_acceso (también los rechazados). Atómico por QR (bloqueo pesimista).
     */
    ValidacionResponseDTO validarIngreso(String idEvento, String idPersonalAcceso, ValidacionRequestDTO body);

    /** Intentos de validación de un evento (ADMIN: cualquiera; ORGANIZADOR: solo el suyo). */
    List<ValidacionRegistroDTO> listarValidaciones(String idEvento, String idUsuario, boolean admin);
}
