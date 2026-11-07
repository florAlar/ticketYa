package eventos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Artista/banda del show (no comprador). */
@Entity
@Table(name = "participante")
@Getter
@Setter
public class Participante {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    /** ARTISTA | BANDA | ELENCO | … */
    @Column(name = "tipo", length = 64)
    private String tipo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
