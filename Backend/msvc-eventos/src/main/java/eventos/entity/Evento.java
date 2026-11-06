package eventos.entity;

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
@Table(name = "evento")
@Getter
@Setter
public class Evento {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    /** Ref lógica a usuarios.usuario (sin FK cross-DB). */
    @Column(name = "id_organizador", length = 36, nullable = false)
    private String idOrganizador;

    @Column(name = "nombre", length = 255, nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo", length = 64, nullable = false)
    private String tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lugar", nullable = false)
    private Lugar lugar;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "es_alta_demanda", nullable = false)
    private Boolean esAltaDemanda;
}
