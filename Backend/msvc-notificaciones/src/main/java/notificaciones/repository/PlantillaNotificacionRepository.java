package notificaciones.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.entity.PlantillaNotificacion;

public interface PlantillaNotificacionRepository extends JpaRepository<PlantillaNotificacion, String> {

    Optional<PlantillaNotificacion> findFirstByTipoEventoAndCanalAndActivaTrue(String tipoEvento, String canal);

    Optional<PlantillaNotificacion> findFirstByTipoEventoAndActivaTrue(String tipoEvento);
}
