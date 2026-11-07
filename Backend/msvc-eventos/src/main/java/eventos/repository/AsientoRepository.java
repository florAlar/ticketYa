package eventos.repository;

import eventos.entity.Asiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsientoRepository extends JpaRepository<Asiento, String> {

    List<Asiento> findBySectorIdOrderByFilaAscNumeroAsc(String idSector);
}
