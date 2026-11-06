package eventos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asiento")
@Getter
@Setter
public class Asiento {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sector", nullable = false)
    private Sector sector;

    @Column(name = "fila", length = 16)
    private String fila;

    @Column(name = "numero")
    private Integer numero;

    @Column(name = "codigo", length = 32)
    private String codigo;
}
