package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportes del organizador (E2-REP-01). Definiciones:
 * <ul>
 *   <li><b>vendidas</b>: unidades de compras APROBADAS (bruto).
 *   <li><b>devueltas</b>: entradas DEVUELTA; <b>aReintegrar</b>: entradas de un evento cancelado con reintegro pendiente.
 *   <li><b>vigentes</b> = vendidas - devueltas - aReintegrar; <b>ocupación</b> = vigentes / capacidad.
 *   <li><b>recaudación neta</b> = bruta - devuelta - aReintegrar.
 * </ul>
 */
public final class ReporteDTO {

    private ReporteDTO() {}

    @Schema(name = "ReporteSector", description = "Ventas, devoluciones, ocupación y recaudación de un sector")
    public record Sector(
            @Schema(description = "UUID del sector (null en la fila de totales)") String idSector,
            @Schema(description = "Nombre del sector", example = "Platea") String nombre,
            @Schema(description = "Capacidad comercial del sector", example = "25000") int capacidad,
            @Schema(description = "Entradas vendidas (compras aprobadas, bruto)", example = "120") long vendidas,
            @Schema(description = "Entradas devueltas", example = "6") long devueltas,
            @Schema(description = "Entradas pendientes de reintegro por cancelación del evento", example = "0") long aReintegrar,
            @Schema(description = "Entradas vigentes = vendidas - devueltas - aReintegrar", example = "114") long vigentes,
            @Schema(description = "Entradas que ya ingresaron (validadas en la puerta)", example = "30") long ingresaron,
            @Schema(description = "Porcentaje de ocupación = vigentes / capacidad * 100", example = "0.46") double ocupacionPct,
            @Schema(description = "Recaudación bruta (compras aprobadas)", example = "21600000.00") BigDecimal recaudacionBruta,
            @Schema(description = "Monto de entradas devueltas", example = "1080000.00") BigDecimal recaudacionDevuelta,
            @Schema(description = "Monto pendiente de reintegro por cancelación", example = "0.00") BigDecimal recaudacionAReintegrar,
            @Schema(description = "Recaudación neta = bruta - devuelta - aReintegrar", example = "20520000.00") BigDecimal recaudacionNeta) {}

    @Schema(name = "ReporteEvento", description = "Reporte completo del evento: totales y detalle por sector")
    public record Evento(
            @Schema(description = "UUID del evento") String idEvento,
            @Schema(description = "Nombre del evento", example = "Metallica - M72 World Tour") String nombre,
            @Schema(description = "Estado del evento", example = "PUBLICADO") String estado,
            @Schema(description = "Moneda de los importes", example = "ARS") String moneda,
            @Schema(description = "Momento de generación (UTC)") LocalDateTime generadoEn,
            @Schema(description = "Compras aprobadas del evento", example = "57") long compras,
            @Schema(description = "Totales de todos los sectores") Sector total,
            @Schema(description = "Detalle por sector") List<Sector> sectores) {}

    @Schema(name = "ReporteVentas", description = "Entradas vendidas y devueltas del evento")
    public record Ventas(
            String idEvento,
            @Schema(description = "Compras aprobadas", example = "57") long compras,
            @Schema(description = "Entradas vendidas (bruto)", example = "120") long entradasVendidas,
            @Schema(description = "Entradas devueltas", example = "6") long entradasDevueltas,
            @Schema(description = "Entradas pendientes de reintegro", example = "0") long entradasAReintegrar,
            @Schema(description = "Entradas vigentes", example = "114") long entradasVigentes,
            @Schema(description = "Entradas que ya ingresaron", example = "30") long ingresaron) {}

    @Schema(name = "ReporteOcupacionSector", description = "Ocupación de un sector")
    public record OcupacionSector(
            String idSector,
            String nombre,
            @Schema(example = "25000") int capacidad,
            @Schema(example = "114") long vigentes,
            @Schema(example = "0.46") double ocupacionPct) {}

    @Schema(name = "ReporteOcupacion", description = "Porcentaje de ocupación del evento y por sector")
    public record Ocupacion(
            String idEvento,
            @Schema(description = "Capacidad total del evento", example = "85000") int capacidadTotal,
            @Schema(description = "Entradas vigentes", example = "114") long vigentes,
            @Schema(description = "Ocupación total en %", example = "0.13") double ocupacionPct,
            List<OcupacionSector> sectores) {}

    @Schema(name = "ReporteRecaudacionSector", description = "Recaudación de un sector")
    public record RecaudacionSector(
            String idSector, String nombre, BigDecimal bruta, BigDecimal devuelta, BigDecimal aReintegrar, BigDecimal neta) {}

    @Schema(name = "ReporteRecaudacion", description = "Recaudación del evento: bruta, devuelta, a reintegrar y neta")
    public record Recaudacion(
            String idEvento,
            @Schema(example = "ARS") String moneda,
            BigDecimal bruta,
            BigDecimal devuelta,
            BigDecimal aReintegrar,
            BigDecimal neta,
            List<RecaudacionSector> sectores) {}
}
