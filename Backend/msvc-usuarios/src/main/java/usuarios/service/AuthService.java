package usuarios.service;

import usuarios.dto.AuthDTO;

public interface AuthService {

    AuthDTO.LoginResponse login(AuthDTO.LoginRequest request);

    AuthDTO.LoginResponse register(AuthDTO.RegisterRequest request);

    AuthDTO.LoginResponse refresh(String bearerToken);

    void logout();
}
