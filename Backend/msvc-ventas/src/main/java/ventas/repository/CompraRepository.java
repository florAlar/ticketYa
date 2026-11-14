package ventas.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ventas.entity.Compra;

public interface CompraRepository extends JpaRepository<Compra, String> {

    List<Compra> findByIdClienteOrderByFechaCompraDesc(String idCliente);

    Optional<Compra> findByReservaIdReserva(String idReserva);

    /**
     * Clientes con compra en el estado indicado para un evento.
     * Uso interno: fan-out de notificaciones push ante cancelación (msvc-notificaciones).
     */
    @Query(
            """
            SELECT DISTINCT c.idCliente
            FROM Compra c
              JOIN c.reserva r
            WHERE r.idEvento = :idEvento
              AND c.estado = :estado
            ORDER BY c.idCliente
            """)
    List<String> findDistinctClientesByEventoAndEstado(
            @Param("idEvento") String idEvento,
            @Param("estado") String estado);

    @Query(
            """
            SELECT COALESCE(SUM(d.cantidad), 0)
            FROM Compra c
              JOIN c.reserva r
              JOIN DetalleReserva d ON d.reserva = r
            WHERE c.idCliente = :idCliente
              AND r.idEvento = :idEvento
              AND c.estado IN :estados
            """)
    Long sumCantidadByClienteEventoAndEstados(
            @Param("idCliente") String idCliente,
            @Param("idEvento") String idEvento,
            @Param("estados") Collection<String> estados);

    List<Compra> findByEstadoAndEmisionEstadoOrderByFechaCompraAsc(String estado, String emisionEstado);
}
