package usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import usuarios.dto.AuthDTO;
import usuarios.entity.Rol;
import usuarios.entity.Usuario;
import usuarios.exception.ConflictException;
import usuarios.exception.ResourceNotFoundException;
import usuarios.exception.UnauthorizedException;
import usuarios.mapper.UsuarioMapper;
import usuarios.repository.RolRepository;
import usuarios.repository.UsuarioRepository;
import usuarios.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String USER_ID = "b0000000-0000-4000-8000-000000000001";
    private static final String EMAIL = "demo@ticketya.com";
    private static final String PASSWORD = "demo1234";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.test";

    private Usuario usuarioActivo() {
        Usuario u = new Usuario();
        u.setIdUsuario(USER_ID);
        u.setNombre("Demo");
        u.setApellido("Usuario");
        u.setEmail(EMAIL);
        u.setPasswordHash("hash");
        u.setCiudad("Buenos Aires");
        u.setActivo(true);
        Set<Rol> roles = new HashSet<>();
        Rol rol = new Rol();
        rol.setNombre("CLIENTE");
        roles.add(rol);
        u.setRoles(roles);
        return u;
    }

    private AuthDTO.UserSessionDTO session() {
        return new AuthDTO.UserSessionDTO(USER_ID, EMAIL, "Demo Usuario", "CLIENTE",
                List.of("CLIENTE"), true);
    }

    @BeforeEach
    void setUp() {
        reset(usuarioRepository, rolRepository, passwordEncoder, jwtUtil, usuarioMapper);
    }

    @Test
    void login_ok_returnsSession() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioActivo()));
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(usuarioMapper.toSession(any(Usuario.class))).thenReturn(session());
        when(jwtUtil.generateToken(eq(USER_ID), eq(EMAIL), eq("Demo Usuario"), eq("CLIENTE"),
                eq(List.of("CLIENTE")))).thenReturn(TOKEN);

        AuthDTO.LoginResponse res = authService.login(new AuthDTO.LoginRequest(EMAIL, PASSWORD));

        assertNotNull(res);
        assertEquals(TOKEN, res.token());
        assertEquals("Bearer", res.type());
        assertEquals(session(), res.user());
    }

    @Test
    void login_emailConEspaciosYMayusculas_seNormaliza() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioActivo()));
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(usuarioMapper.toSession(any(Usuario.class))).thenReturn(session());
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(),
                any(List.class))).thenReturn(TOKEN);

        authService.login(new AuthDTO.LoginRequest("  Demo@TicketYa.COM ", PASSWORD));

        verify(usuarioRepository).findByEmail(EMAIL);
    }

    @Test
    void login_usuarioNoEncontrado_throwsUnauthorized() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(new AuthDTO.LoginRequest(EMAIL, PASSWORD)));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void login_cuentaDesactivada_throwsUnauthorized() {
        Usuario inactivo = usuarioActivo();
        inactivo.setActivo(false);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(inactivo));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(new AuthDTO.LoginRequest(EMAIL, PASSWORD)));

        assertEquals("Cuenta desactivada. Contactá soporte.", ex.getMessage());
    }

    @Test
    void login_passwordIncorrecto_throwsUnauthorized() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioActivo()));
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(new AuthDTO.LoginRequest(EMAIL, PASSWORD)));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void register_ok_returnsSession() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioActivo());
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(usuarioActivo()));
        when(usuarioMapper.toSession(any(Usuario.class))).thenReturn(session());
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(),
                any(List.class))).thenReturn(TOKEN);

        AuthDTO.LoginResponse res = authService.register(
                new AuthDTO.RegisterRequest("Ana", EMAIL, PASSWORD, "Pérez", "Rosario"));

        assertNotNull(res);
        assertEquals(TOKEN, res.token());
    }

    @Test
    void register_emailYaExiste_throwsConflict() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> authService.register(new AuthDTO.RegisterRequest("Ana", EMAIL, PASSWORD, null, null)));

        assertEquals("Ya existe una cuenta con ese email", ex.getMessage());
    }

    @Test
    void register_rolClienteNoConfigurado_throwsResourceNotFound() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.register(new AuthDTO.RegisterRequest("Ana", EMAIL, PASSWORD, null, null)));

        assertEquals("Rol CLIENTE no configurado", ex.getMessage());
    }

    @Test
    void refresh_tokenValido_returnsSession() {
        when(jwtUtil.validateToken(TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(usuarioActivo()));
        when(usuarioMapper.toSession(any(Usuario.class))).thenReturn(session());
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(),
                any(List.class))).thenReturn(TOKEN);

        AuthDTO.LoginResponse res = authService.refresh("Bearer " + TOKEN);

        assertNotNull(res);
        assertEquals(TOKEN, res.token());
    }

    @Test
    void refresh_headerNulo_throwsUnauthorized() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh(null));

        assertEquals("Token inválido o expirado", ex.getMessage());
    }

    @Test
    void refresh_headerSinBearer_seTrataComoToken() {
        when(jwtUtil.validateToken("tok")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("tok"));

        assertEquals("Token inválido o expirado", ex.getMessage());
    }

    @Test
    void refresh_tokenInvalido_throwsUnauthorized() {
        when(jwtUtil.validateToken(TOKEN)).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("Bearer " + TOKEN));

        assertEquals("Token inválido o expirado", ex.getMessage());
    }

    @Test
    void refresh_usuarioNoEncontrado_throwsUnauthorized() {
        when(jwtUtil.validateToken(TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("Bearer " + TOKEN));

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void refresh_cuentaDesactivada_throwsUnauthorized() {
        Usuario inactivo = usuarioActivo();
        inactivo.setActivo(false);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(usuarioRepository.findWithRolesByIdUsuario(USER_ID)).thenReturn(Optional.of(inactivo));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("Bearer " + TOKEN));

        assertEquals("Cuenta desactivada", ex.getMessage());
    }

    @Test
    void logout_noOp() {
        // Stateless JWT: logout es no-op en E1; verificar que no lanza excepción.
        authService.logout();
    }
}
