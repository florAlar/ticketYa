package preferencias.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import preferencias.document.InteraccionEvento;

public interface InteraccionEventoRepository extends MongoRepository<InteraccionEvento, String> {

    List<InteraccionEvento> findByIdCliente(String idCliente);

    List<InteraccionEvento> findByIdEvento(String idEvento);
}
