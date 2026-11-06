package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, String> {
}
