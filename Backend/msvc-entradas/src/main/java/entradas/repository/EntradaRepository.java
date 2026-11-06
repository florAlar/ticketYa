package entradas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import entradas.entity.Entrada;

public interface EntradaRepository extends JpaRepository<Entrada, String> {
}
