package usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import usuarios.dto.RolesUpdateDTO;
import usuarios.dto.UsuarioResponseDTO;
import usuarios.dto.UsuarioUpdateDTO;
import usuarios.entity.Rol;
import usuarios.entity.Usuario;
import usuarios.exception.ResourceNotFoundException;
import usuarios.mapper.UsuarioMapper;
import usuarios.repository.RolRepository;
import usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private static final String USER_ID = "b0000000-0000-4000-8000-000000000001";

    private Usuario usuario(String id, String email, boolean activo, Set<Rol> roles) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        u.setEmail(email);
        u.setActivo(activo);
        u.setRoles(roles);
        return u;
    }

    private Set<Rol> rolesCliente() {
        Set<Rol> roles = new HashSet<>();
        Rol r = new Rol();
        r.setNombre("CLIENTE");
        roles.add(r);
        return roles;
    }

    @BeforeEach
    void setUp() {
        reset(usuarioRepository, rolRepository, usuarioMapper);
    }

    @Test
    void me_ok_returnsResponse() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(usuarioMapper.toResponse(u)).thenReturn(dto);

        UsuarioResponseDTO res = usuarioService.me(USER_ID);

        assertNotNull(res);
        assertEquals(USER_ID, res.getId());
    }

    @Test
    void me_usuarioNoEncontrado_throwsResourceNotFound() {
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.me(USER_ID));

        assertEquals("Usuario no encontrado: " + USER_ID, ex.getMessage());
    }

    @Test
    void patchMe_actualizaCamposNoNulos() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        UsuarioUpdateDTO body = new UsuarioUpdateDTO();
        body.setNombre("Nuevo");
        body.setApellido(null);
        body.setCiudad("Rosario");
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(u)).thenReturn(u);
        when(usuarioMapper.toResponse(u)).thenReturn(dto);

        UsuarioResponseDTO res = usuarioService.patchMe(USER_ID, body);

        assertNotNull(res);
        assertEquals("Nuevo", u.getNombre());
        assertEquals("Rosario", u.getCiudad());
    }

    @Test
    void patchMe_camposNulos_noSeActualizan() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        u.setNombre("Viejo");
        u.setCiudad("Vieja");
        UsuarioUpdateDTO body = new UsuarioUpdateDTO();
        body.setNombre(null);
        body.setApellido("");
        body.setCiudad(null);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(u)).thenReturn(u);
        when(usuarioMapper.toResponse(u)).thenReturn(new UsuarioResponseDTO());

        usuarioService.patchMe(USER_ID, body);

        assertEquals("Viejo", u.getNombre());
        assertEquals("Vieja", u.getCiudad());
    }

    @Test
    void findAll_mapeaLista() {
        Usuario u1 = usuario("1", "a@b.com", true, rolesCliente());
        Usuario u2 = usuario("2", "c@d.com", false, rolesCliente());
        when(usuarioRepository.findAll()).thenReturn(List.of(u1, u2));
        when(usuarioRepository.findWithRolesByIdUsuario("1")).thenReturn(Optional.of(u1));
        when(usuarioRepository.findWithRolesByIdUsuario("2")).thenReturn(Optional.of(u2));
        when(usuarioMapper.toResponse(u1)).thenReturn(new UsuarioResponseDTO());
        when(usuarioMapper.toResponse(u2)).thenReturn(new UsuarioResponseDTO());

        List<UsuarioResponseDTO> res = usuarioService.findAll();

        assertEquals(2, res.size());
    }

    @Test
    void findById_ok_returnsResponse() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(usuarioMapper.toResponse(u)).thenReturn(dto);

        UsuarioResponseDTO res = usuarioService.findById(USER_ID);

        assertNotNull(res);
        assertEquals(USER_ID, res.getId());
    }

    @Test
    void patchEstado_activoTrue_guarda() {
        Usuario u = usuario(USER_ID, "a@b.com", false, rolesCliente());
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(u)).thenReturn(u);
        when(usuarioMapper.toResponse(u)).thenReturn(dto);

        UsuarioResponseDTO res = usuarioService.patchEstado(USER_ID, true);

        assertNotNull(res);
        assertEquals(true, u.getActivo());
    }

    @Test
    void patchEstado_activoFalse_guarda() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(u)).thenReturn(u);
        when(usuarioMapper.toResponse(u)).thenReturn(new UsuarioResponseDTO());

        usuarioService.patchEstado(USER_ID, false);

        assertEquals(false, u.getActivo());
    }

    @Test
    void getRoles_returnsRoleNames() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));

        List<String> res = usuarioService.getRoles(USER_ID);

        assertEquals(List.of("CLIENTE"), res);
    }

    @Test
    void putRoles_reemplazaRoles() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        Rol rolOrg = new Rol();
        rolOrg.setNombre("ORGANIZADOR");
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(rolRepository.findByNombre("ORGANIZADOR")).thenReturn(Optional.of(rolOrg));
        when(usuarioRepository.save(u)).thenReturn(u);

        List<String> res = usuarioService.putRoles(USER_ID, new RolesUpdateDTO(List.of("ORGANIZADOR")));

        assertEquals(List.of("ORGANIZADOR"), res);
        assertEquals(1, u.getRoles().size());
        assertEquals("ORGANIZADOR", u.getRoles().iterator().next().getNombre());
    }

    @Test
    void putRoles_accesoSeNormalizaAPersonalAcceso() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        Rol rolAcceso = new Rol();
        rolAcceso.setNombre("PERSONAL_ACCESO");
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(rolRepository.findByNombre("PERSONAL_ACCESO")).thenReturn(Optional.of(rolAcceso));
        when(usuarioRepository.save(u)).thenReturn(u);

        List<String> res = usuarioService.putRoles(USER_ID, new RolesUpdateDTO(List.of("ACCESO")));

        assertEquals(List.of("PERSONAL_ACCESO"), res);
        verify(rolRepository).findByNombre("PERSONAL_ACCESO");
    }

    @Test
    void putRoles_rolNoEncontrado_throwsResourceNotFound() {
        Usuario u = usuario(USER_ID, "a@b.com", true, rolesCliente());
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(u));
        when(rolRepository.findByNombre("INEXISTENTE")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.putRoles(USER_ID, new RolesUpdateDTO(List.of("INEXISTENTE"))));

        assertEquals("Rol no encontrado: INEXISTENTE", ex.getMessage());
    }
}
