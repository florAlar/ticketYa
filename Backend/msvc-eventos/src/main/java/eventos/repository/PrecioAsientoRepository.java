package eventos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import eventos.entity.PrecioAsiento;

public interface PrecioAsientoRepository extends JpaRepository<PrecioAsiento, String> {
}
