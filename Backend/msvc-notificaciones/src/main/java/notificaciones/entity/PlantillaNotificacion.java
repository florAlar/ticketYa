package notificaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plantilla_notificacion")
@Getter
@Setter
public class PlantillaNotificacion {

    @Id
    @Column(name = "id_plantilla", length = 36, nullable = false)
    private String idPlantilla;

    @Column(name = "tipo_evento", length = 64, nullable = false)
    private String tipoEvento;

    @Column(name = "canal", length = 32, nullable = false)
    private String canal;

    @Column(name = "asunto", length = 255, nullable = false)
    private String asunto;

    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "activa", nullable = false)
    private Boolean activa;
}
