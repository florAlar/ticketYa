package eventos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "precio_sector")
@Getter
@Setter
public class PrecioSector {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sector", nullable = false)
    private Sector sector;

    @Column(name = "precio", precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    /**
     * Cupo comercial del evento en este sector (LUG-04). Null = toda la capacidad del sector. En
     * sectores numerados siempre es toda la capacidad (se venden asientos concretos).
     */
    @Column(name = "cupo")
    private Integer cupo;

    /** Cupo que efectivamente se vende: el propio del evento o, si no tiene, la capacidad del sector. */
    public Integer cupoEfectivo() {
        if (cupo != null) {
            return cupo;
        }
        return sector != null ? sector.getCapacidad() : null;
    }
}
