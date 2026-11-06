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
@Table(name = "pago")
@Getter
@Setter
public class Pago {

    @Id
    @Column(name = "id_pago", length = 36, nullable = false)
    private String idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @Column(name = "proveedor", length = 64, nullable = false)
    private String proveedor;

    @Column(name = "id_transaccion_externa", length = 128, nullable = false, unique = true)
    private String idTransaccionExterna;

    @Column(name = "monto", precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
}
