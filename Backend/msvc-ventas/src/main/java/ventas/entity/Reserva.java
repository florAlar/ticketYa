package ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reserva")
@Getter
@Setter
public class Reserva {

    @Id
    @Column(name = "id_reserva", length = 36, nullable = false)
    private String idReserva;

    /** Ref lógica usuarios.usuario */
    @Column(name = "id_cliente", length = 36, nullable = false)
    private String idCliente;

    /** Ref lógica eventos.evento */
    @Column(name = "id_evento", length = 36, nullable = false)
    private String idEvento;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;

    @Column(name = "total", precision = 12, scale = 2, nullable = false)
    private BigDecimal total;
}
