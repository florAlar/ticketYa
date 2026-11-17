package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ArtistasRequest", description = "Artistas favoritos del cliente")
public class ArtistasRequestDTO {

    @NotNull(message = "artistas es obligatorio")
    private List<String> artistas;
}
