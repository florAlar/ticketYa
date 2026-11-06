package usuarios.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import usuarios.dto.AuthDTO;
import usuarios.dto.UsuarioResponseDTO;
import usuarios.entity.Rol;
import usuarios.entity.Usuario;

@Component
public class UsuarioMapper {

    public UsuarioResponseDTO toResponse(Usuario u) {
        List<String> roles = roleNames(u);
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getIdUsuario());
        dto.setEmail(u.getEmail());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setCiudad(u.getCiudad());
        dto.setActivo(Boolean.TRUE.equals(u.getActivo()));
        dto.setRoles(roles);
        dto.setRol(primaryRole(roles));
        return dto;
    }

    public AuthDTO.UserSessionDTO toSession(Usuario u) {
        List<String> roles = roleNames(u);
        String displayName = (u.getNombre() + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
        return new AuthDTO.UserSessionDTO(
                u.getIdUsuario(),
                u.getEmail(),
                displayName.isBlank() ? u.getNombre() : displayName,
                primaryRole(roles),
                roles,
                Boolean.TRUE.equals(u.getActivo()));
    }

    public static List<String> roleNames(Usuario u) {
        if (u.getRoles() == null || u.getRoles().isEmpty()) {
            return List.of();
        }
        return u.getRoles().stream()
                .map(Rol::getNombre)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public static String primaryRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "CLIENTE";
        }
        // prioridad útil para multi-rol futuro
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("ORGANIZADOR")) {
            return "ORGANIZADOR";
        }
        if (roles.contains("PERSONAL_ACCESO")) {
            return "PERSONAL_ACCESO";
        }
        return roles.get(0);
    }
}
