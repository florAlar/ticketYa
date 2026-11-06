package ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ventas.entity.Compra;

public interface CompraRepository extends JpaRepository<Compra, String> {
}
