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

    /** Reportes: [idSector, unidades, importe] de las compras en ese estado del evento, por sector. */
    @Query(
            """
            SELECT d.idSector, SUM(d.cantidad), SUM(d.cantidad * d.precioUnitario)
            FROM Compra c
              JOIN c.reserva r
              JOIN DetalleReserva d ON d.reserva = r
            WHERE r.idEvento = :idEvento
              AND c.estado = :estado
            GROUP BY d.idSector
            """)
    List<Object[]> ventasPorSector(@Param("idEvento") String idEvento, @Param("estado") String estado);

    @Query(
            """
            SELECT COUNT(c)
            FROM Compra c
              JOIN c.reserva r
            WHERE r.idEvento = :idEvento
              AND c.estado = :estado
            """)
    long contarPorEventoYEstado(@Param("idEvento") String idEvento, @Param("estado") String estado);

    List<Compra> findByEstadoAndEmisionEstadoOrderByFechaCompraAsc(String estado, String emisionEstado);
}
