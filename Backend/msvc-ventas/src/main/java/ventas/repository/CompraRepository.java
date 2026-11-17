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

    /**
     * Backfill del historial (msvc-preferencias): compras en un estado con cliente, evento,
     * fecha, importe y cantidad de entradas. Uso interno, no expuesto por el gateway.
     */
    @Query(
            """
            SELECT c.idCompra, c.idCliente, r.idEvento, c.fechaCompra, c.total, c.moneda,
                   COALESCE(SUM(d.cantidad), 0)
            FROM Compra c
              JOIN c.reserva r
              LEFT JOIN DetalleReserva d ON d.reserva = r
            WHERE c.estado = :estado
              AND (:desde IS NULL OR c.fechaCompra >= :desde)
              AND (:hasta IS NULL OR c.fechaCompra <= :hasta)
            GROUP BY c.idCompra, c.idCliente, r.idEvento, c.fechaCompra, c.total, c.moneda
            ORDER BY c.fechaCompra ASC
            """)
    List<Object[]> findComprasInternas(
            @Param("estado") String estado,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);
}
