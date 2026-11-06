package preferencias.document;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colección interacciones_eventos — un documento por interacción.
 * idCliente / idEvento = refs lógicas MySQL.
 * Índices: init.js.
 */
@Document(collection = "interacciones_eventos")
@Getter
@Setter
public class InteraccionEvento {

    @Id
    private String id;

    private String idCliente;

    private String idEvento;

    private String tipo;

    private String categoria;

    private Instant fechaEvento;

    private Instant fechaCompra;

    private BigDecimal monto;
}
