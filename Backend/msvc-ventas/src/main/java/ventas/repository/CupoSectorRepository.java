package ventas.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ventas.entity.CupoSector;

public interface CupoSectorRepository extends JpaRepository<CupoSector, String> {

    List<CupoSector> findByIdEvento(String idEvento);

    List<CupoSector> findByIdEventoIn(Collection<String> idEventos);

    Optional<CupoSector> findByIdEventoAndIdSector(String idEvento, String idSector);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CupoSector c WHERE c.idEvento = :idEvento AND c.idSector = :idSector")
    Optional<CupoSector> findByEventoAndSectorForUpdate(
            @Param("idEvento") String idEvento, @Param("idSector") String idSector);
}
