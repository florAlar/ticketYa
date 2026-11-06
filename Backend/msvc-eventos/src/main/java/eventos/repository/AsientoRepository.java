package eventos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import eventos.entity.Asiento;

public interface AsientoRepository extends JpaRepository<Asiento, String> {
}
