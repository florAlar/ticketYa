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
@Table(name = "notificacion")
@Getter
@Setter
public class Notificacion {

    @Id
    @Column(name = "id_notificacion", length = 36, nullable = false)
    private String idNotificacion;

    /** Ref lógica usuarios.usuario */
    @Column(name = "id_usuario", length = 36, nullable = false)
    private String idUsuario;

    /** Ref lógica eventos.evento */
    @Column(name = "id_evento_origen", length = 36)
    private String idEventoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla")
    private PlantillaNotificacion plantilla;

    @Column(name = "tipo", length = 64, nullable = false)
    private String tipo;

    @Column(name = "canal", length = 32, nullable = false)
    private String canal;

    @Column(name = "asunto", length = 255, nullable = false)
    private String asunto;

    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;
}
