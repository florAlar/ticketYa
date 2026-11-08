package ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "compra")
@Getter
@Setter
public class Compra {

    @Id
    @Column(name = "id_compra", length = 36, nullable = false)
    private String idCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    /** Ref lógica usuarios.usuario */
    @Column(name = "id_cliente", length = 36, nullable = false)
    private String idCliente;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(name = "total", precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;

    /**
     * Emisión de QR en msvc-entradas tras pago aprobado.
     * NO_REQUIERE | PENDIENTE | EMITIDA | FALLIDA
     */
    @Column(name = "emision_estado", length = 16, nullable = false)
    private String emisionEstado;

    @Column(name = "emision_intentos", nullable = false)
    private Integer emisionIntentos;

    @Column(name = "emision_ultimo_error", length = 255)
    private String emisionUltimoError;

    @Column(name = "emision_actualizado")
    private LocalDateTime emisionActualizado;
}
