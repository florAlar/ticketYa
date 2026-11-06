package ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, String> {
}
