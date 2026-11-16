package ventas.service;

import ventas.dto.ReporteDTO;

/** E2-REP-01: reportes del organizador sobre un evento. */
public interface ReporteService {

    /** Reporte completo: totales y detalle por sector. */
    ReporteDTO.Evento reporte(String idEvento, String idUsuario, String rol);

    ReporteDTO.Ventas ventas(String idEvento, String idUsuario, String rol);

    java.util.List<ReporteDTO.Sector> sectores(String idEvento, String idUsuario, String rol);

    ReporteDTO.Ocupacion ocupacion(String idEvento, String idUsuario, String rol);

    ReporteDTO.Recaudacion recaudacion(String idEvento, String idUsuario, String rol);
}
