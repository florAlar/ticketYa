package entradas.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import entradas.entity.Entrada;

public interface EntradaRepository extends JpaRepository<Entrada, String> {

    List<Entrada> findByIdClienteOrderByFechaEmisionDesc(String idCliente);

    List<Entrada> findByIdCompra(String idCompra);

    Optional<Entrada> findByCodigoQr(String codigoQr);

    List<Entrada> findByIdEventoAndEstado(String idEvento, String estado);

    long countByIdCompra(String idCompra);
}
