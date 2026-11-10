package ventas.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ventas.entity.AsientoReservado;

public interface AsientoReservadoRepository extends JpaRepository<AsientoReservado, String> {

    List<AsientoReservado> findByIdEvento(String idEvento);

    @Query("SELECT a FROM AsientoReservado a WHERE a.reserva.idReserva = :idReserva ORDER BY a.fila, a.numero")
    List<AsientoReservado> findByReservaIdReserva(@Param("idReserva") String idReserva);

    boolean existsByIdEventoAndIdSectorAndFilaAndNumero(
            String idEvento, String idSector, String fila, Integer numero);

    Optional<AsientoReservado> findByIdEventoAndIdSectorAndFilaAndNumero(
            String idEvento, String idSector, String fila, Integer numero);

    /** Libera los asientos de una reserva cancelada, rechazada o vencida. */
    @Modifying
    @Query("DELETE FROM AsientoReservado a WHERE a.reserva.idReserva = :idReserva")
    int eliminarPorReserva(@Param("idReserva") String idReserva);

    /** Pasa los asientos de la reserva a VENDIDO cuando el pago se aprueba. */
    @Modifying
    @Query("UPDATE AsientoReservado a SET a.estado = 'VENDIDO' WHERE a.reserva.idReserva = :idReserva")
    int marcarVendidos(@Param("idReserva") String idReserva);
}
