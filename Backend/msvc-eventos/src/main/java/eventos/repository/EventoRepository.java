package eventos.repository;

import eventos.entity.Evento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, String> {

    /** LUG-01: un lugar con eventos (de cualquier estado) no se puede eliminar. */
    boolean existsByLugarId(String idLugar);

    @EntityGraph(attributePaths = "lugar")
    @Query(
            value = """
                    select e from Evento e
                    join e.lugar l
                    where (:tipo is null or lower(e.tipo) = lower(:tipo))
                      and (:ciudad is null or lower(l.ciudad) like lower(concat('%', :ciudad, '%')))
                      and (:organizador is null or e.idOrganizador = :organizador)
                      and (
                        :estado is null
                        or (
                          :catalogoDefault = true
                          and upper(e.estado) in ('PUBLICADO', 'AGOTADO')
                        )
                        or (
                          :catalogoDefault = false
                          and lower(e.estado) = lower(:estado)
                        )
                      )
                      and (:fechaDesde is null or e.fechaInicio >= :fechaDesde)
                      and (:fechaHasta is null or e.fechaInicio <= :fechaHasta)
                      and (
                        :participante is null
                        or exists (
                          select 1 from EventoParticipante ep
                          where ep.evento = e and ep.participante.id = :participante
                        )
                      )
                    """,
            countQuery = """
                    select count(e) from Evento e
                    join e.lugar l
                    where (:tipo is null or lower(e.tipo) = lower(:tipo))
                      and (:ciudad is null or lower(l.ciudad) like lower(concat('%', :ciudad, '%')))
                      and (:organizador is null or e.idOrganizador = :organizador)
                      and (
                        :estado is null
                        or (
                          :catalogoDefault = true
                          and upper(e.estado) in ('PUBLICADO', 'AGOTADO')
                        )
                        or (
                          :catalogoDefault = false
                          and lower(e.estado) = lower(:estado)
                        )
                      )
                      and (:fechaDesde is null or e.fechaInicio >= :fechaDesde)
                      and (:fechaHasta is null or e.fechaInicio <= :fechaHasta)
                      and (
                        :participante is null
                        or exists (
                          select 1 from EventoParticipante ep
                          where ep.evento = e and ep.participante.id = :participante
                        )
                      )
                    """)
    Page<Evento> buscar(
            @Param("tipo") String tipo,
            @Param("ciudad") String ciudad,
            @Param("organizador") String organizador,
            @Param("estado") String estado,
            @Param("catalogoDefault") boolean catalogoDefault,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("participante") String participante,
            Pageable pageable);

    @EntityGraph(attributePaths = "lugar")
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> findByIdWithLugar(@Param("id") String id);

    @EntityGraph(attributePaths = "lugar")
    List<Evento> findByIdOrganizadorOrderByFechaInicioDesc(String idOrganizador);
}
