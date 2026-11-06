package notificaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "intento_envio")
@Getter
@Setter
public class IntentoEnvio {

    @Id
    @Column(name = "id_intento", length = 36, nullable = false)
    private String idIntento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_notificacion", nullable = false)
    private Notificacion notificacion;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    @Column(name = "fecha_intento", nullable = false)
    private LocalDateTime fechaIntento;

    @Column(name = "resultado", length = 32, nullable = false)
    private String resultado;

    @Column(name = "detalle_error", length = 500)
    private String detalleError;
}
