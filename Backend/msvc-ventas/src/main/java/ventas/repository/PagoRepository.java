package ventas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, String> {

    List<Pago> findByCompraIdCompraOrderByFechaPagoDesc(String idCompra);
}
