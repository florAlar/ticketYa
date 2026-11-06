package entradas.entity;

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
@Table(name = "validacion_acceso")
@Getter
@Setter
public class ValidacionAcceso {

    @Id
    @Column(name = "id_validacion", length = 36, nullable = false)
    private String idValidacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrada", nullable = false)
    private Entrada entrada;

    /** Ref lógica usuarios.usuario PERSONAL_ACCESO */
    @Column(name = "id_personal_acceso", length = 36, nullable = false)
    private String idPersonalAcceso;

    @Column(name = "fecha_validacion", nullable = false)
    private LocalDateTime fechaValidacion;

    @Column(name = "resultado", length = 32, nullable = false)
    private String resultado;

    @Column(name = "dispositivo", length = 128)
    private String dispositivo;
}
