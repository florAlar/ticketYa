package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Página simple de catálogo (sin acoplar el shape Spring Page al front). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {

    private List<T> content;

    @Schema(description = "Página actual (0-based)", example = "0")
    private int page;

    @Schema(description = "Tamaño de página", example = "20")
    private int size;

    @Schema(description = "Total de elementos", example = "42")
    private long totalElements;

    @Schema(description = "Total de páginas", example = "3")
    private int totalPages;
}
