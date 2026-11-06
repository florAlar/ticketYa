package eventos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lugar")
@Getter
@Setter
public class Lugar {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "ciudad", length = 120, nullable = false)
    private String ciudad;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;
}
