package usuarios.service;

import java.util.List;
import usuarios.dto.RolesUpdateDTO;
import usuarios.dto.UsuarioResponseDTO;
import usuarios.dto.UsuarioUpdateDTO;

public interface UsuarioService {

    UsuarioResponseDTO me(String userId);

    UsuarioResponseDTO patchMe(String userId, UsuarioUpdateDTO body);

    List<UsuarioResponseDTO> findAll();

    UsuarioResponseDTO findById(String id);

    UsuarioResponseDTO patchEstado(String id, boolean activo);

    List<String> getRoles(String id);

    List<String> putRoles(String id, RolesUpdateDTO body);
}
