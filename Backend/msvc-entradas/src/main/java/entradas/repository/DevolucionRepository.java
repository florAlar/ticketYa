package entradas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import entradas.entity.Devolucion;

public interface DevolucionRepository extends JpaRepository<Devolucion, String> {
}
