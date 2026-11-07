package entradas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Shape cercano al FE domain/entrada (toEntradaListItem). */
@Getter
@Setter
public class EntradaResponseDTO {

    private String id;
    /** Null si la política de QR no permite exhibirlo. */
    private String codigoQR;
    private String idCompra;
    private String idCliente;
    private String idEvento;
    private String idSector;
    private String estado;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaIngreso;
    /** Opcional: no está en schema entrada; se rellena en emisión si viene. */
    private BigDecimal precio;
    private String eventoNombre;
    /** Estado del evento (Feign); p.ej. CANCELADO → sin QR. */
    private String eventoEstado;
    private String sectorNombre;
    private String lugarNombre;
    private String lugarCiudad;
}
