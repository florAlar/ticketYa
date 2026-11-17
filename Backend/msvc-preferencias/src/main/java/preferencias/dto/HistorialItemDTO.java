package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Ítem del historial de un cliente, con el snapshot del evento. */
@Getter
@Setter
@Schema(name = "HistorialItem", description = "Compra del cliente con snapshot del evento")
public class HistorialItemDTO {

    private String id;
    private String tipo;
    private String idEvento;
    private String nombreEvento;
    private String categoria;
    private String ciudad;
    private List<String> artistas;
    private Instant fechaEvento;
    private Instant fechaCompra;
    private BigDecimal monto;
    private Integer cantidadEntradas;
}
