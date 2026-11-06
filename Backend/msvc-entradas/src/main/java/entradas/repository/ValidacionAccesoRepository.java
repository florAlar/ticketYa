package entradas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import entradas.entity.ValidacionAcceso;

public interface ValidacionAccesoRepository extends JpaRepository<ValidacionAcceso, String> {
}
