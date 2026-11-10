package ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Asiento virtual TOMADO de un sector numerado. Los asientos libres no se persisten: solo existe
 * una fila por asiento reservado o vendido. La clave única (evento, sector, fila, número) impide
 * que un mismo asiento quede tomado dos veces.
 */
@Entity
@Table(name = "asiento_reservado")
@Getter
@Setter
public class AsientoReservado {

    @Id
    @Column(name = "id_asiento_reservado", length = 36, nullable = false)
    private String idAsientoReservado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    /** Ref lógica eventos.evento */
    @Column(name = "id_evento", length = 36, nullable = false)
    private String idEvento;

    /** Ref lógica eventos.sector */
    @Column(name = "id_sector", length = 36, nullable = false)
    private String idSector;

    @Column(name = "fila", length = 8, nullable = false)
    private String fila;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    /** RESERVADO | VENDIDO */
    @Column(name = "estado", length = 16, nullable = false)
    private String estado;
}
