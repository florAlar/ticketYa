package usuarios.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usuarios.dto.RolesUpdateDTO;
import usuarios.dto.UsuarioResponseDTO;
import usuarios.dto.UsuarioUpdateDTO;
import usuarios.entity.Rol;
import usuarios.entity.Usuario;
import usuarios.exception.ResourceNotFoundException;
import usuarios.mapper.UsuarioMapper;
import usuarios.repository.RolRepository;
import usuarios.repository.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO me(String userId) {
        return usuarioMapper.toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO patchMe(String userId, UsuarioUpdateDTO body) {
        Usuario u = requireUser(userId);
        if (body.getNombre() != null && !body.getNombre().isBlank()) {
            u.setNombre(body.getNombre().trim());
        }
        if (body.getApellido() != null) {
            u.setApellido(body.getApellido().trim());
        }
        if (body.getCiudad() != null) {
            u.setCiudad(body.getCiudad().trim());
        }
        return usuarioMapper.toResponse(usuarioRepository.save(u));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> findAll() {
        return usuarioRepository.findAll().stream()
                .map(u -> usuarioRepository.findWithRolesByIdUsuario(u.getIdUsuario()).orElse(u))
                .map(usuarioMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO findById(String id) {
        return usuarioMapper.toResponse(requireUser(id));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO patchEstado(String id, boolean activo) {
        Usuario u = requireUser(id);
        u.setActivo(activo);
        return usuarioMapper.toResponse(usuarioRepository.save(u));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRoles(String id) {
        return UsuarioMapper.roleNames(requireUser(id));
    }

    @Override
    @Transactional
    public List<String> putRoles(String id, RolesUpdateDTO body) {
        Usuario u = requireUser(id);
        Set<Rol> roles = new HashSet<>();
        for (String name : body.roles()) {
            String n = name.trim().toUpperCase();
            if ("ACCESO".equals(n)) {
                n = "PERSONAL_ACCESO";
            }
            final String rolNombre = n;
            Rol rol = rolRepository
                    .findByNombre(rolNombre)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + rolNombre));
            roles.add(rol);
        }
        u.setRoles(roles);
        usuarioRepository.save(u);
        return UsuarioMapper.roleNames(requireUser(id));
    }

    private Usuario requireUser(String id) {
        return usuarioRepository
                .findWithRolesByIdUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }
}
