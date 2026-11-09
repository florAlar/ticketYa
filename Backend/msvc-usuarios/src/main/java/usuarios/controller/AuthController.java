package usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usuarios.dto.AuthDTO;
import usuarios.exception.ApiError;
import usuarios.service.AuthService;

/**
 * Auth pública (vía gateway: /api/auth/** → StripPrefix → /auth/**).
 * Patrón usersejemplo adaptado a TicketYa.
 */
@Tag(name = "Auth", description = "Registro, login, refresh y logout. Vía gateway: /api/auth/**.")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Iniciar sesión",
            description = """
                    Valida credenciales (BCrypt) y emite un JWT con userId, email, rol y roles.
                    Endpoint público.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada",
                    content = @Content(schema = @Schema(implementation = AuthDTO.LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta desactivada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @Operation(
            summary = "Registrar cliente",
            description = "Alta pública. Solo crea cuentas con rol CLIENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta creada y sesión iniciada",
                    content = @Content(schema = @Schema(implementation = AuthDTO.LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthDTO.LoginResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest body) {
        return ResponseEntity.ok(authService.register(body));
    }

    @Operation(
            summary = "Renovar sesión",
            description = "Re-emite el JWT si el Bearer recibido es válido y la cuenta sigue activa.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado",
                    content = @Content(schema = @Schema(implementation = AuthDTO.LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token inválido, expirado o cuenta desactivada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthDTO.LoginResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(authService.refresh(authorization));
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Stateless: el cliente descarta el token. Responde 204 (no-op en E1).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Sesión cerrada (sin cuerpo)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
