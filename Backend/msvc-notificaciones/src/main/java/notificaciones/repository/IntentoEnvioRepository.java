package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.entity.IntentoEnvio;

public interface IntentoEnvioRepository extends JpaRepository<IntentoEnvio, String> {
}
