package ventas.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ventas.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, String> {

    List<Reserva> findByIdClienteAndIdEventoAndEstadoIn(
            String idCliente, String idEvento, Collection<String> estados);

    @Query(
            """
            SELECT r FROM Reserva r
            WHERE r.estado IN :estados
              AND r.fechaExpiracion < :now
            """)
    List<Reserva> findExpiradas(
            @Param("estados") Collection<String> estados, @Param("now") LocalDateTime now);
}
