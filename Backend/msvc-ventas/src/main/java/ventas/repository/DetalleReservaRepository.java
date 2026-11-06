package ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.DetalleReserva;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, String> {
}
