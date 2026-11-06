package eventos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import eventos.entity.Sector;

public interface SectorRepository extends JpaRepository<Sector, String> {
}
