package entradas.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import entradas.entity.Devolucion;

public interface DevolucionRepository extends JpaRepository<Devolucion, String> {

    @Query(
            """
            SELECT d FROM Devolucion d JOIN FETCH d.entrada e
            WHERE d.estado = :estado
            ORDER BY d.fechaSolicitud ASC
            """)
    List<Devolucion> findByEstadoOrderByFechaSolicitudAsc(@Param("estado") String estado);

    @Query(
            """
            SELECT d FROM Devolucion d JOIN FETCH d.entrada e
            WHERE e.idCliente = :idCliente
            ORDER BY d.fechaSolicitud DESC
            """)
    List<Devolucion> findByEntradaIdClienteOrderByFechaSolicitudDesc(
            @Param("idCliente") String idCliente);

    @Query(
            """
            SELECT d FROM Devolucion d JOIN FETCH d.entrada e
            WHERE e.idEntrada = :idEntrada
            """)
    List<Devolucion> findByEntradaIdEntrada(@Param("idEntrada") String idEntrada);

    @Query(
            """
            SELECT d FROM Devolucion d JOIN FETCH d.entrada e
            WHERE d.idDevolucion = :id
            """)
    Optional<Devolucion> findByIdWithEntrada(@Param("id") String id);
}
