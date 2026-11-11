package usuarios.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import usuarios.observability.Bitacora;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ROL_CLIENTE = "CLIENTE";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UsuarioMapper usuarioMapper;

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository
                .findByEmail(email)
                .orElseThrow(() -> {
                    Bitacora.registrar("LOGIN_FALLIDO", null, email, "motivo=credenciales");
                    return new UnauthorizedException("Credenciales inválidas");
                });

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            Bitacora.registrar("LOGIN_FALLIDO", usuario.getIdUsuario(), email, "motivo=cuenta_desactivada");
            throw new UnauthorizedException("Cuenta desactivada. Contactá soporte.");
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            Bitacora.registrar("LOGIN_FALLIDO", usuario.getIdUsuario(), email, "motivo=credenciales");
            throw new UnauthorizedException("Credenciales inválidas");
        }
        Bitacora.registrar("LOGIN_OK", usuario.getIdUsuario(), email, "-");
        return issue(usuario);
    }

    @Override
    @Transactional
    public AuthDTO.LoginResponse register(AuthDTO.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con ese email");
        }

        Rol cliente = rolRepository
                .findByNombre(ROL_CLIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no configurado"));

        Usuario u = new Usuario();
        u.setIdUsuario(UUID.randomUUID().toString());
        u.setNombre(request.nombre().trim());
        u.setApellido(request.apellido() != null ? request.apellido().trim() : "");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(request.password()));
        u.setCiudad(request.ciudad() != null ? request.ciudad().trim() : null);
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        Set<Rol> roles = new HashSet<>();
        roles.add(cliente);
        u.setRoles(roles);

        u = usuarioRepository.save(u);
        // recargar con roles
        u = usuarioRepository.findWithRolesByIdUsuario(u.getIdUsuario()).orElse(u);
        Bitacora.registrar("REGISTRO_OK", u.getIdUsuario(), email, "rol=" + ROL_CLIENTE);
        return issue(u);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDTO.LoginResponse refresh(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Token inválido o expirado");
        }
        String userId = jwtUtil.extractUserId(token);
        Usuario u = usuarioRepository
                .findWithRolesByIdUsuario(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new UnauthorizedException("Cuenta desactivada");
        }
        return issue(u);
    }

    @Override
    public void logout() {
        // Stateless JWT: el cliente descarta el token. Placeholder para blacklist E2.
    }

    private AuthDTO.LoginResponse issue(Usuario usuario) {
        AuthDTO.UserSessionDTO session = usuarioMapper.toSession(usuario);
        List<String> roles = session.roles();
        String token = jwtUtil.generateToken(
                session.id(),
                session.email(),
                session.nombre(),
                session.rol(),
                roles);
        return new AuthDTO.LoginResponse(token, session);
    }

    private static String stripBearer(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return header.trim();
    }
}
