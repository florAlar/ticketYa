package entradas.service;

import java.util.List;
import entradas.dto.DevolucionCreateRequestDTO;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;

public interface EntradaService {

    List<EntradaResponseDTO> emitir(EmitirEntradasRequestDTO request);

    List<EntradaResponseDTO> listarMias(String idCliente);

    EntradaResponseDTO obtenerMia(String idCliente, String idEntrada);

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
}
