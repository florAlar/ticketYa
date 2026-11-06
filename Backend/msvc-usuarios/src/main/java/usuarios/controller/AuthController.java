package usuarios.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usuarios.dto.AuthDTO;
import usuarios.service.AuthService;

/**
 * Auth pública (vía gateway: /api/auth/** → StripPrefix → /auth/**).
 * Patrón usersejemplo adaptado a TicketYa.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.LoginResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest body) {
        return ResponseEntity.ok(authService.register(body));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDTO.LoginResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(authService.refresh(authorization));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
