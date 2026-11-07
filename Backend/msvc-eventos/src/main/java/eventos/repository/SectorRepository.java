package eventos.repository;

import eventos.entity.Sector;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, String> {

    List<Sector> findByLugarIdOrderByNombreAsc(String idLugar);
}
