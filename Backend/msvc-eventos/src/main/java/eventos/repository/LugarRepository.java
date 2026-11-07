package eventos.repository;

import eventos.entity.Lugar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LugarRepository extends JpaRepository<Lugar, String> {

    List<Lugar> findByCiudadContainingIgnoreCaseOrderByNombreAsc(String ciudad);

    List<Lugar> findAllByOrderByNombreAsc();
}
