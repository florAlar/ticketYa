package ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, String> {
}
