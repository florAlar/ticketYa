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
}
