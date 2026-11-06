package eventos.repository;

import eventos.entity.Evento;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, String> {

    @EntityGraph(attributePaths = "lugar")
    @Query(
            value = """
                    select e from Evento e
                    join e.lugar l
                    where (:tipo is null or lower(e.tipo) = lower(:tipo))
                      and (:ciudad is null or lower(l.ciudad) like lower(concat('%', :ciudad, '%')))
                      and (:organizador is null or e.idOrganizador = :organizador)
                      and (:estado is null or lower(e.estado) = lower(:estado))
                      and (:fechaDesde is null or e.fechaInicio >= :fechaDesde)
                      and (:fechaHasta is null or e.fechaInicio <= :fechaHasta)
                    """,
            countQuery = """
                    select count(e) from Evento e
                    join e.lugar l
                    where (:tipo is null or lower(e.tipo) = lower(:tipo))
                      and (:ciudad is null or lower(l.ciudad) like lower(concat('%', :ciudad, '%')))
                      and (:organizador is null or e.idOrganizador = :organizador)
                      and (:estado is null or lower(e.estado) = lower(:estado))
                      and (:fechaDesde is null or e.fechaInicio >= :fechaDesde)
                      and (:fechaHasta is null or e.fechaInicio <= :fechaHasta)
                    """)
    Page<Evento> buscar(
            @Param("tipo") String tipo,
            @Param("ciudad") String ciudad,
            @Param("organizador") String organizador,
            @Param("estado") String estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);

    @EntityGraph(attributePaths = "lugar")
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> findByIdWithLugar(@Param("id") String id);
}
