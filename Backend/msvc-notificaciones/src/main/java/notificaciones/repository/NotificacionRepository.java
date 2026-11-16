package notificaciones.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import notificaciones.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    /** Idempotencia del consumidor: no repetir la notificación del mismo origen al mismo usuario. */
    boolean existsByIdUsuarioAndClaveIdempotencia(String idUsuario, String claveIdempotencia);

    Page<Notificacion> findByIdUsuarioOrderByFechaCreacionDesc(String idUsuario, Pageable pageable);

    Page<Notificacion> findByIdUsuarioAndLeidaOrderByFechaCreacionDesc(
            String idUsuario, Boolean leida, Pageable pageable);

    Optional<Notificacion> findByIdNotificacionAndIdUsuario(String idNotificacion, String idUsuario);

    long countByIdUsuarioAndLeidaFalse(String idUsuario);

    long countByEstado(String estado);

    /**
     * Toma un lote de notificaciones pendientes con lock pesimista.
     * {@code jakarta.persistence.lock.timeout=-2} (SKIP LOCKED en MySQL 8) evita que dos
     * instancias del job envíen la misma notificación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select n from Notificacion n where n.estado = 'PENDIENTE' order by n.fechaCreacion asc")
    List<Notificacion> findPendientesParaEnvio(Pageable pageable);
}
