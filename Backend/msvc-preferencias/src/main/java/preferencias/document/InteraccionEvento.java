package preferencias.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colección interacciones_eventos — un documento por interacción.
 * idCliente / idEvento = refs lógicas MySQL.
 * Índices: init.js + anotaciones (se crean también en volúmenes existentes).
 */
@Document(collection = "interacciones_eventos")
@CompoundIndexes({
        @CompoundIndex(name = "ix_interacciones_cliente_fecha", def = "{'idCliente': 1, 'fechaEvento': -1}"),
        @CompoundIndex(name = "ix_interacciones_evento_tipo", def = "{'idEvento': 1, 'tipo': 1}"),
        @CompoundIndex(name = "ix_interacciones_cliente_tipo", def = "{'idCliente': 1, 'tipo': 1}")
})
@Getter
@Setter
public class InteraccionEvento {

    @Id
    private String id;

    @Indexed(name = "ix_interacciones_idCliente")
    private String idCliente;

    @Indexed(name = "ix_interacciones_idEvento")
    private String idEvento;

    /** COMPRA | ASISTENCIA | VISUALIZACION … */
    private String tipo;

    /** Snapshot del evento al momento de la interacción (evita N+1 al mostrar historial). */
    private String nombreEvento;

    private String categoria;

    private String ciudad;

    private List<String> artistas = new ArrayList<>();

    private Instant fechaEvento;

    private Instant fechaCompra;

    private BigDecimal monto;

    private Integer cantidadEntradas;

    /** Idempotencia de la ingesta (msvc-ventas → RabbitMQ). */
    @Indexed(name = "uk_interacciones_idCompra", unique = true, sparse = true)
    private String idCompra;
}
