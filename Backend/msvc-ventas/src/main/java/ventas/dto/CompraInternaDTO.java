package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Compra aprobada con datos mínimos para ingesta/backfill del historial (uso interno). */
@Getter
@Setter
@Schema(name = "CompraInterna", description = "Compra aprobada para procesos internos")
public class CompraInternaDTO {

    private String idCompra;
    private String idCliente;
    private String idEvento;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String moneda;
    private Integer cantidadEntradas;
}
