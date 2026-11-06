package usuarios.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import usuarios.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, String> {

    Optional<Rol> findByNombre(String nombre);
}
