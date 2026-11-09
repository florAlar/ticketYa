package usuarios.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import usuarios.dto.AuthDTO.UserSessionDTO;
import usuarios.dto.UsuarioResponseDTO;
import usuarios.entity.Rol;
import usuarios.entity.Usuario;

class UsuarioMapperTest {

    private static final String USER_ID = "b0000000-0000-4000-8000-000000000001";
    private static final String EMAIL = "demo@ticketya.com";

    private Usuario usuarioConRoles(String... rolesNombres) {
        Usuario u = new Usuario();
        u.setIdUsuario(USER_ID);
        u.setNombre("Ana");
        u.setApellido("Pérez");
        u.setEmail(EMAIL);
        u.setCiudad("Rosario");
        u.setActivo(true);
        Set<Rol> roles = new HashSet<>();
        for (String nombre : rolesNombres) {
            Rol r = new Rol();
            r.setNombre(nombre);
            roles.add(r);
        }
        u.setRoles(roles);
        return u;
    }

    private Usuario usuarioSinRoles() {
        Usuario u = new Usuario();
        u.setIdUsuario(USER_ID);
        u.setNombre("Ana");
        u.setApellido(null);
        u.setEmail(EMAIL);
        u.setActivo(true);
        u.setRoles(null);
        return u;
    }

    @Test
    void toResponse_mapeaTodosLosCampos() {
        UsuarioMapper mapper = new UsuarioMapper();
        Usuario u = usuarioConRoles("CLIENTE", "ORGANIZADOR");
        UsuarioResponseDTO dto = mapper.toResponse(u);

        assertNotNull(dto);
        assertEquals(USER_ID, dto.getId());
        assertEquals(EMAIL, dto.getEmail());
        assertEquals("Ana", dto.getNombre());
        assertEquals("Pérez", dto.getApellido());
        assertEquals("Rosario", dto.getCiudad());
        assertTrue(dto.isActivo());
        assertEquals(List.of("CLIENTE", "ORGANIZADOR"), dto.getRoles());
        assertEquals("ORGANIZADOR", dto.getRol());
    }

    @Test
    void toResponse_sinRoles_rolDefaultCliente() {
        UsuarioMapper mapper = new UsuarioMapper();
        Usuario u = usuarioSinRoles();
        UsuarioResponseDTO dto = mapper.toResponse(u);

        assertNotNull(dto);
        assertEquals(List.of(), dto.getRoles());
        assertEquals("CLIENTE", dto.getRol());
    }

    @Test
    void toSession_mapeaSesion() {
        UsuarioMapper mapper = new UsuarioMapper();
        Usuario u = usuarioConRoles("CLIENTE");
        UserSessionDTO session = mapper.toSession(u);

        assertNotNull(session);
        assertEquals(USER_ID, session.id());
        assertEquals(EMAIL, session.email());
        assertEquals("Ana Pérez", session.nombre());
        assertEquals("CLIENTE", session.rol());
        assertEquals(List.of("CLIENTE"), session.roles());
        assertTrue(session.activo());
    }

    @Test
    void toSession_sinApellido_usaNombre() {
        UsuarioMapper mapper = new UsuarioMapper();
        Usuario u = usuarioSinRoles();
        u.setApellido(null);
        UserSessionDTO session = mapper.toSession(u);

        assertEquals("Ana", session.nombre());
    }

    @Test
    void roleNames_null_returnsEmpty() {
        Usuario u = new Usuario();
        u.setRoles(null);
        List<String> res = UsuarioMapper.roleNames(u);

        assertEquals(List.of(), res);
    }

    @Test
    void roleNames_empty_returnsEmpty() {
        Usuario u = new Usuario();
        u.setRoles(new HashSet<>());
        List<String> res = UsuarioMapper.roleNames(u);

        assertEquals(List.of(), res);
    }

    @Test
    void roleNames_returnsSorted() {
        Usuario u = usuarioConRoles("ORGANIZADOR", "CLIENTE");
        List<String> res = UsuarioMapper.roleNames(u);

        assertEquals(List.of("CLIENTE", "ORGANIZADOR"), res);
    }

    @Test
    void primaryRole_vacio_returnsCliente() {
        assertEquals("CLIENTE", UsuarioMapper.primaryRole(List.of()));
    }

    @Test
    void primaryRole_null_returnsCliente() {
        assertEquals("CLIENTE", UsuarioMapper.primaryRole(null));
    }

    @Test
    void primaryRole_admin_returnsAdmin() {
        assertEquals("ADMIN", UsuarioMapper.primaryRole(List.of("CLIENTE", "ADMIN")));
    }

    @Test
    void primaryRole_organizador_returnsOrganizador() {
        assertEquals("ORGANIZADOR", UsuarioMapper.primaryRole(List.of("CLIENTE", "ORGANIZADOR")));
    }

    @Test
    void primaryRole_personalAcceso_returnsPersonalAcceso() {
        assertEquals("PERSONAL_ACCESO",
                UsuarioMapper.primaryRole(List.of("CLIENTE", "PERSONAL_ACCESO")));
    }

    @Test
    void primaryRole_otro_returnsPrimero() {
        assertEquals("CLIENTE", UsuarioMapper.primaryRole(List.of("CLIENTE", "OTRO")));
    }
}
