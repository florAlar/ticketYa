package preferencias.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import preferencias.document.InteraccionEvento;

public interface InteraccionEventoRepository extends MongoRepository<InteraccionEvento, String> {

    List<InteraccionEvento> findByIdCliente(String idCliente);

    List<InteraccionEvento> findByIdEvento(String idEvento);

    Optional<InteraccionEvento> findByIdCompra(String idCompra);

    List<InteraccionEvento> findByIdClienteAndTipoOrderByFechaCompraDesc(String idCliente, String tipo);

    Page<InteraccionEvento> findByIdClienteAndTipo(String idCliente, String tipo, Pageable pageable);

    List<InteraccionEvento> findByIdEventoAndTipo(String idEvento, String tipo);

    List<InteraccionEvento> findByIdEventoInAndTipo(Collection<String> idEventos, String tipo);

    List<InteraccionEvento> findByIdClienteInAndTipo(Collection<String> idClientes, String tipo);
}
