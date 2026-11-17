package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Página simple para listados del microservicio. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Page", description = "Página de resultados")
public class PageResponseDTO<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
