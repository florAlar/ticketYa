package ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cupo_sector")
@Getter
@Setter
public class CupoSector {

    @Id
    @Column(name = "id_cupo", length = 36, nullable = false)
    private String idCupo;

    /** Ref lógica eventos.evento */
    @Column(name = "id_evento", length = 36, nullable = false)
    private String idEvento;

    /** Ref lógica eventos.sector */
    @Column(name = "id_sector", length = 36, nullable = false)
    private String idSector;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    @Column(name = "cantidad_reservada", nullable = false)
    private Integer cantidadReservada;

    @Column(name = "cantidad_vendida", nullable = false)
    private Integer cantidadVendida;
}
