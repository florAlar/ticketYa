package notificaciones.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import notificaciones.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    /** Idempotencia del consumidor: no repetir la notificación del mismo origen al mismo usuario. */
    boolean existsByIdUsuarioAndClaveIdempotencia(String idUsuario, String claveIdempotencia);

    /** Filas PUSH/EMAIL que componen un mismo mensaje (misma clave de mensaje). */
    List<Notificacion> findByIdUsuarioAndClaveMensaje(String idUsuario, String claveMensaje);

    /**
     * Canales de varias claves de mensaje en una sola consulta, para informar "PUSH + EMAIL" sin
     * caer en N+1 al listar la bandeja.
     */
    @Query("""
            select n.claveMensaje, n.canal
            from Notificacion n
            where n.idUsuario = :idUsuario
              and n.claveMensaje in :claves
            """)
    List<Object[]> findCanalesPorClaves(
            @Param("idUsuario") String idUsuario, @Param("claves") Collection<String> claves);

    /**
     * Bandeja: una entrada por mensaje. Si el mensaje tiene PUSH y EMAIL se muestra la fila PUSH;
     * la fila EMAIL solo aparece cuando el mensaje se entregó únicamente por email.
     */
    @Query("""
            select n from Notificacion n
            where n.idUsuario = :idUsuario
              and not (n.canal = 'EMAIL'
                       and n.claveMensaje is not null
                       and exists (
                           select 1 from Notificacion p
                           where p.idUsuario = n.idUsuario
                             and p.claveMensaje = n.claveMensaje
                             and p.canal = 'PUSH'))
            order by n.fechaCreacion desc
            """)
    Page<Notificacion> findVisiblesByIdUsuario(
            @Param("idUsuario") String idUsuario, Pageable pageable);

    @Query("""
            select n from Notificacion n
            where n.idUsuario = :idUsuario
              and n.leida = :leida
              and not (n.canal = 'EMAIL'
                       and n.claveMensaje is not null
                       and exists (
                           select 1 from Notificacion p
                           where p.idUsuario = n.idUsuario
                             and p.claveMensaje = n.claveMensaje
                             and p.canal = 'PUSH'))
            order by n.fechaCreacion desc
            """)
    Page<Notificacion> findVisiblesByIdUsuarioAndLeida(
            @Param("idUsuario") String idUsuario,
            @Param("leida") Boolean leida,
            Pageable pageable);

    /** Badge de la campanita: mensajes (no filas) visibles sin leer. */
    @Query("""
            select count(n) from Notificacion n
            where n.idUsuario = :idUsuario
              and n.leida = false
              and not (n.canal = 'EMAIL'
                       and n.claveMensaje is not null
                       and exists (
                           select 1 from Notificacion p
                           where p.idUsuario = n.idUsuario
                             and p.claveMensaje = n.claveMensaje
                             and p.canal = 'PUSH'))
            """)
    long countVisiblesNoLeidas(@Param("idUsuario") String idUsuario);

    Optional<Notificacion> findByIdNotificacionAndIdUsuario(String idNotificacion, String idUsuario);

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
