package usuarios.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import usuarios.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findWithRolesByIdUsuario(String idUsuario);

    boolean existsByEmail(String email);
}
