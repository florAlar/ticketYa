package preferencias.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import preferencias.document.PreferenciasCliente;

public interface PreferenciasClienteRepository extends MongoRepository<PreferenciasCliente, String> {

    Optional<PreferenciasCliente> findByIdCliente(String idCliente);
}
