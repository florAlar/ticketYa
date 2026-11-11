package entradas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import entradas.entity.ValidacionAcceso;

public interface ValidacionAccesoRepository extends JpaRepository<ValidacionAcceso, String> {

    /** Validaciones (aceptadas y rechazadas) de las entradas de un evento, la más reciente primero. */
    List<ValidacionAcceso> findByEntrada_IdEventoOrderByFechaValidacionDesc(String idEvento);
}
