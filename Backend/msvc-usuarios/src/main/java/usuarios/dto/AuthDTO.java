package usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTOs de autenticación (login/register/refresh). Shape alineado al FE AuthProvider. */
public final class AuthDTO {

    private AuthDTO() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) String nombre,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            String apellido,
            String ciudad) {
    }

    /** Respuesta que consume el FE: { token, user }. */
    public record LoginResponse(String token, String type, UserSessionDTO user) {
        public LoginResponse(String token, UserSessionDTO user) {
            this(token, "Bearer", user);
        }
    }

    public record UserSessionDTO(
            String id,
            String email,
            String nombre,
            String rol,
            List<String> roles,
            boolean activo) {
    }
}
