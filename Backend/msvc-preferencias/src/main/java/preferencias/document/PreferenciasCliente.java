package preferencias.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colección preferencias_clientes — un documento por cliente.
 * idCliente = ref lógica usuarios.usuario (sin FK).
 * Índices: init.js (uk idCliente).
 */
@Document(collection = "preferencias_clientes")
@Getter
@Setter
public class PreferenciasCliente {

    @Id
    private String id;

    private String idCliente;

    private List<String> categorias = new ArrayList<>();

    private List<String> ciudades = new ArrayList<>();

    private RangoPrecio rangoPrecio;

    private List<String> artistas = new ArrayList<>();

    private Instant fechaActualizacion;
}
