package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    /** Idempotencia del consumidor: no repetir la notificación del mismo evento al mismo usuario. */
    boolean existsByIdUsuarioAndIdEventoOrigenAndTipo(
            String idUsuario, String idEventoOrigen, String tipo);
}
