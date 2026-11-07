package eventos.repository;

import eventos.entity.Participante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipanteRepository extends JpaRepository<Participante, String> {

    @Query(
            """
            select p from Participante p
            where (:q is null or lower(p.nombre) like lower(concat('%', :q, '%')))
              and (:tipo is null or lower(p.tipo) = lower(:tipo))
            order by p.nombre asc
            """)
    List<Participante> buscar(@Param("q") String q, @Param("tipo") String tipo);
}
