package eventos.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoParticipanteId implements Serializable {

    private String evento;
    private String participante;

    public EventoParticipanteId() {}

    public EventoParticipanteId(String evento, String participante) {
        this.evento = evento;
        this.participante = participante;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventoParticipanteId that)) {
            return false;
        }
        return Objects.equals(evento, that.evento) && Objects.equals(participante, that.participante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(evento, participante);
    }
}
