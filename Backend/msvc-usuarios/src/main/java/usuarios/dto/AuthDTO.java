package usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTOs de autenticación (login/register/refresh). Shape alineado al FE AuthProvider. */
public final class AuthDTO {

    private AuthDTO() {
    }

    @Schema(name = "LoginRequest", description = "Credenciales de acceso")
    public record LoginRequest(
            @Schema(description = "Email registrado", example = "demo@ticketya.com")
            @NotBlank @Email String email,
            @Schema(description = "Contraseña", example = "demo1234")
            @NotBlank String password) {
    }

    @Schema(name = "RegisterRequest", description = "Alta pública de cliente")
    public record RegisterRequest(
            @Schema(description = "Nombre", example = "Ana")
            @NotBlank @Size(min = 2, max = 100) String nombre,
            @Schema(description = "Email único", example = "ana@example.com")
            @NotBlank @Email String email,
            @Schema(description = "Contraseña (mínimo 6 caracteres)", example = "secreto123")
            @NotBlank @Size(min = 6, max = 100) String password,
            @Schema(description = "Apellido", example = "Pérez")
            String apellido,
            @Schema(description = "Ciudad", example = "Buenos Aires")
            String ciudad) {
    }

    /** Respuesta que consume el FE: { token, user }. */
    @Schema(name = "LoginResponse", description = "Sesión emitida: token + datos del usuario")
    public record LoginResponse(
            @Schema(description = "JWT a enviar como Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
            String token,
            @Schema(description = "Tipo de token", example = "Bearer")
            String type,
            @Schema(description = "Datos de sesión")
            UserSessionDTO user) {
        public LoginResponse(String token, UserSessionDTO user) {
            this(token, "Bearer", user);
        }
    }

    @Schema(name = "UserSession", description = "Usuario autenticado y sus roles")
    public record UserSessionDTO(
            @Schema(description = "UUID del usuario", example = "b0000000-0000-4000-8000-000000000001")
            String id,
            @Schema(description = "Email", example = "demo@ticketya.com")
            String email,
            @Schema(description = "Nombre", example = "Usuario Demo")
            String nombre,
            @Schema(description = "Rol principal", example = "CLIENTE")
            String rol,
            @Schema(description = "Todos los roles del usuario", example = "[\"CLIENTE\"]")
            List<String> roles,
            @Schema(description = "Cuenta activa", example = "true")
            boolean activo) {
    }
}
