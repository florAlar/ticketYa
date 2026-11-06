package usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rol")
@Getter
@Setter
public class Rol {

    @Id
    @Column(name = "id_rol", length = 36, nullable = false)
    private String idRol;

    @Column(name = "nombre", length = 64, nullable = false, unique = true)
    private String nombre;
}
