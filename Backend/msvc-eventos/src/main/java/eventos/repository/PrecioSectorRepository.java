package eventos.repository;

import eventos.entity.PrecioSector;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrecioSectorRepository extends JpaRepository<PrecioSector, String> {

    @Query("""
            select p.evento.id, min(p.precio)
            from PrecioSector p
            where p.evento.id in :ids
              and p.activo = true
            group by p.evento.id
            """)
    List<Object[]> minPrecioByEventoIds(@Param("ids") Collection<String> ids);

    @Query("""
            select min(p.precio)
            from PrecioSector p
            where p.evento.id = :eventoId
              and p.activo = true
            """)
    BigDecimal minPrecioByEventoId(@Param("eventoId") String eventoId);

    @Query("""
            select p from PrecioSector p
            join fetch p.sector
            where p.evento.id = :eventoId
              and p.activo = true
            order by p.precio asc
            """)
    List<PrecioSector> findActivosByEventoId(@Param("eventoId") String eventoId);

    @Query("""
            select p from PrecioSector p
            join fetch p.sector
            where p.evento.id = :eventoId
            order by p.sector.nombre asc
            """)
    List<PrecioSector> findAllByEventoId(@Param("eventoId") String eventoId);

    Optional<PrecioSector> findByEventoIdAndSectorId(String eventoId, String sectorId);

    List<PrecioSector> findByEventoId(String eventoId);
}
