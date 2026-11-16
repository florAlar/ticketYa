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

    /** Email de destino resuelto al crear la notificación (canal EMAIL). */
    @Column(name = "destinatario", length = 255)
    private String destinatario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;

    /** Leída por el usuario en la bandeja de la app (distinto de ENVIADA). */
    @Column(name = "leida", nullable = false)
    private Boolean leida = Boolean.FALSE;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    /**
     * Clave de idempotencia del origen: {@code tipo:eventoId} para eventos y
     * {@code CompraConfirmada:compraId} para compras. Permite que un cliente reciba dos compras
     * del mismo evento sin que el consumer duplique avisos ante reintentos.
     */
    @Column(name = "clave_idempotencia", length = 128)
    private String claveIdempotencia;
}
