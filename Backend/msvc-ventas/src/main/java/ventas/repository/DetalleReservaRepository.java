package ventas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.DetalleReserva;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, String> {

    List<DetalleReserva> findByReservaIdReserva(String idReserva);
}
