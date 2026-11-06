package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.entity.PlantillaNotificacion;

public interface PlantillaNotificacionRepository extends JpaRepository<PlantillaNotificacion, String> {
}
