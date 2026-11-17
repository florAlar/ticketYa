package eventos.repository;

import eventos.entity.EventoParticipante;
import eventos.entity.EventoParticipanteId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, EventoParticipanteId> {

    @EntityGraph(attributePaths = "participante")
    @Query(
            """
            select ep from EventoParticipante ep
            where ep.evento.id = :idEvento
            order by ep.orden asc, ep.participante.nombre asc
            """)
    List<EventoParticipante> findByEventoIdOrdered(@Param("idEvento") String idEvento);

    /** PAR-01: un participante asociado a algún evento no se puede eliminar. */
    boolean existsByParticipanteId(String idParticipante);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EventoParticipante ep where ep.evento.id = :idEvento")
    void deleteByEventoId(@Param("idEvento") String idEvento);

    @Query(
            """
            select ep.evento.id from EventoParticipante ep
            where ep.evento.id in :ids
            """)
    List<String> findEventoIdsWithParticipantes(@Param("ids") List<String> ids);

    /** Catálogo con artistas: pares [idEvento, nombreParticipante] sin N+1. */
    @Query(
            """
            select ep.evento.id, ep.participante.nombre
            from EventoParticipante ep
            where ep.evento.id in :ids
            order by ep.evento.id asc, ep.orden asc
            """)
    List<Object[]> findArtistasByEventoIds(@Param("ids") List<String> ids);
}
