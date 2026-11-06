package ventas.dto;

import jakarta.validation.constraints.NotBlank;

public record CompraCreateRequestDTO(@NotBlank String idReserva) {
}
