package ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.CupoSector;

public interface CupoSectorRepository extends JpaRepository<CupoSector, String> {
}
