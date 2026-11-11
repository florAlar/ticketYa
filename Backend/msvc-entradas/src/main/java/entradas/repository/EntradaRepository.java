package entradas.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import entradas.entity.Entrada;

public interface EntradaRepository extends JpaRepository<Entrada, String> {

    List<Entrada> findByIdClienteOrderByFechaEmisionDesc(String idCliente);

    List<Entrada> findByIdCompra(String idCompra);

    Optional<Entrada> findByCodigoQr(String codigoQr);

    /**
     * Lectura con bloqueo pesimista (SELECT … FOR UPDATE) para la validación de ingreso: dos
     * escaneos simultáneos del mismo QR se serializan y solo el primero lo marca USADA.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.codigoQr = :codigo")
    Optional<Entrada> findByCodigoQrForUpdate(@Param("codigo") String codigo);

    List<Entrada> findByIdEventoAndEstado(String idEvento, String estado);

    long countByIdCompra(String idCompra);

    /**
     * Reportes: cantidad y suma de precios de las entradas de un evento por sector y estado
     * (fila = [idSector, estado, cantidad, sumaPrecios]).
     */
    @Query("""
            SELECT e.idSector, e.estado, COUNT(e), COALESCE(SUM(e.precio), 0)
            FROM Entrada e
            WHERE e.idEvento = :idEvento
            GROUP BY e.idSector, e.estado
            """)
    List<Object[]> resumenPorSectorYEstado(@Param("idEvento") String idEvento);
}
