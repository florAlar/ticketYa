package eventos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import eventos.entity.Lugar;

public interface LugarRepository extends JpaRepository<Lugar, String> {
}
