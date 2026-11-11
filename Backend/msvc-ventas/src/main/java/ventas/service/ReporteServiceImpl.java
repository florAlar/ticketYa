package ventas.service;

import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ventas.clients.EntradasClient;
import ventas.clients.EntradasClient.ResumenSector;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.clients.EventosClient.SectorFeignDTO;
import ventas.dto.ReporteDTO;
import ventas.entity.CupoSector;
import ventas.exception.ForbiddenException;
import ventas.exception.ResourceNotFoundException;
import ventas.observability.Bitacora;
import ventas.repository.CompraRepository;
import ventas.repository.CupoSectorRepository;

/**
 * Reportes del organizador. Las ventas y la capacidad salen de msvc-ventas; las devoluciones y los
 * ingresos, de msvc-entradas (Feign); el dueño del evento, de msvc-eventos (Feign).
 */
@Service
public class ReporteServiceImpl implements ReporteService {

    static final String COMPRA_APROBADA = "APROBADA";
    private static final String MONEDA_DEFAULT = "ARS";

    private final CompraRepository compraRepository;
    private final CupoSectorRepository cupoSectorRepository;
    private final EventosClient eventosClient;
    private final EntradasClient entradasClient;

    public ReporteServiceImpl(
            CompraRepository compraRepository,
            CupoSectorRepository cupoSectorRepository,
            EventosClient eventosClient,
            EntradasClient entradasClient) {
        this.compraRepository = compraRepository;
        this.cupoSectorRepository = cupoSectorRepository;
        this.eventosClient = eventosClient;
        this.entradasClient = entradasClient;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDTO.Evento reporte(String idEvento, String idUsuario, String rol) {
        EventoDetalleFeignDTO evento = cargarEventoConPermiso(idEvento, idUsuario, rol);
        List<ReporteDTO.Sector> sectores = sectoresDe(idEvento, evento);
        ReporteDTO.Sector total = totales(sectores);
        String moneda = evento.getSectores().stream()
                .map(SectorFeignDTO::getMoneda)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse(MONEDA_DEFAULT);
        long compras = compraRepository.contarPorEventoYEstado(idEvento, COMPRA_APROBADA);
        Bitacora.registrar("REPORTE_CONSULTADO", idUsuario, idEvento, "rol=" + rol);
        return new ReporteDTO.Evento(
                idEvento, evento.getNombre(), evento.getEstado(), moneda,
                LocalDateTime.now(ZoneOffset.UTC), compras, total, sectores);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDTO.Ventas ventas(String idEvento, String idUsuario, String rol) {
        ReporteDTO.Evento r = reporte(idEvento, idUsuario, rol);
        ReporteDTO.Sector t = r.total();
        return new ReporteDTO.Ventas(
                idEvento, r.compras(), t.vendidas(), t.devueltas(), t.aReintegrar(), t.vigentes(), t.ingresaron());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteDTO.Sector> sectores(String idEvento, String idUsuario, String rol) {
        return reporte(idEvento, idUsuario, rol).sectores();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDTO.Ocupacion ocupacion(String idEvento, String idUsuario, String rol) {
        ReporteDTO.Evento r = reporte(idEvento, idUsuario, rol);
        List<ReporteDTO.OcupacionSector> filas = r.sectores().stream()
                .map(s -> new ReporteDTO.OcupacionSector(
                        s.idSector(), s.nombre(), s.capacidad(), s.vigentes(), s.ocupacionPct()))
                .toList();
        ReporteDTO.Sector t = r.total();
        return new ReporteDTO.Ocupacion(idEvento, t.capacidad(), t.vigentes(), t.ocupacionPct(), filas);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDTO.Recaudacion recaudacion(String idEvento, String idUsuario, String rol) {
        ReporteDTO.Evento r = reporte(idEvento, idUsuario, rol);
        List<ReporteDTO.RecaudacionSector> filas = r.sectores().stream()
                .map(s -> new ReporteDTO.RecaudacionSector(
                        s.idSector(), s.nombre(), s.recaudacionBruta(), s.recaudacionDevuelta(),
                        s.recaudacionAReintegrar(), s.recaudacionNeta()))
                .toList();
        ReporteDTO.Sector t = r.total();
        return new ReporteDTO.Recaudacion(
                idEvento, r.moneda(), t.recaudacionBruta(), t.recaudacionDevuelta(),
                t.recaudacionAReintegrar(), t.recaudacionNeta(), filas);
    }

    // ---------------------------------------------------------------- cálculo

    private List<ReporteDTO.Sector> sectoresDe(String idEvento, EventoDetalleFeignDTO evento) {
        Map<String, SectorFeignDTO> delEvento = new HashMap<>();
        Set<String> orden = new LinkedHashSet<>();
        for (SectorFeignDTO s : evento.getSectores()) {
            delEvento.put(s.getId(), s);
            orden.add(s.getId());
        }

        Map<String, CupoSector> cupos = new HashMap<>();
        for (CupoSector c : cupoSectorRepository.findByIdEvento(idEvento)) {
            cupos.put(c.getIdSector(), c);
            orden.add(c.getIdSector());
        }

        Map<String, long[]> unidades = new HashMap<>();
        Map<String, BigDecimal> importes = new HashMap<>();
        for (Object[] fila : compraRepository.ventasPorSector(idEvento, COMPRA_APROBADA)) {
            String sector = (String) fila[0];
            unidades.put(sector, new long[] {((Number) fila[1]).longValue()});
            importes.put(sector, aBigDecimal(fila[2]));
            orden.add(sector);
        }

        Map<String, ResumenSector> entradas = new HashMap<>();
        for (ResumenSector r : resumenEntradas(idEvento)) {
            entradas.put(r.idSector(), r);
            orden.add(r.idSector());
        }

        List<ReporteDTO.Sector> out = new ArrayList<>();
        for (String idSector : orden) {
            SectorFeignDTO s = delEvento.get(idSector);
            CupoSector cupo = cupos.get(idSector);
            ResumenSector e = entradas.get(idSector);
            long vendidas = unidades.containsKey(idSector) ? unidades.get(idSector)[0] : 0L;
            BigDecimal bruta = importes.getOrDefault(idSector, BigDecimal.ZERO);
            long devueltas = e != null ? e.devueltas() : 0L;
            long aReintegrar = e != null ? e.aReintegrar() : 0L;
            long ingresaron = e != null ? e.usadas() : 0L;
            BigDecimal devuelta = e != null && e.montoDevuelto() != null ? e.montoDevuelto() : BigDecimal.ZERO;
            BigDecimal pendiente = e != null && e.montoAReintegrar() != null ? e.montoAReintegrar() : BigDecimal.ZERO;
            int capacidad = cupo != null && cupo.getCapacidadTotal() != null ? cupo.getCapacidadTotal() : 0;
            out.add(fila(idSector, s != null && s.getNombre() != null ? s.getNombre() : idSector,
                    capacidad, vendidas, devueltas, aReintegrar, ingresaron, bruta, devuelta, pendiente));
        }
        return out;
    }

    private static ReporteDTO.Sector totales(List<ReporteDTO.Sector> sectores) {
        int capacidad = 0;
        long vendidas = 0, devueltas = 0, aReintegrar = 0, ingresaron = 0;
        BigDecimal bruta = BigDecimal.ZERO, devuelta = BigDecimal.ZERO, pendiente = BigDecimal.ZERO;
        for (ReporteDTO.Sector s : sectores) {
            capacidad += s.capacidad();
            vendidas += s.vendidas();
            devueltas += s.devueltas();
            aReintegrar += s.aReintegrar();
            ingresaron += s.ingresaron();
            bruta = bruta.add(s.recaudacionBruta());
            devuelta = devuelta.add(s.recaudacionDevuelta());
            pendiente = pendiente.add(s.recaudacionAReintegrar());
        }
        return fila(null, "TOTAL", capacidad, vendidas, devueltas, aReintegrar, ingresaron, bruta, devuelta, pendiente);
    }

    private static ReporteDTO.Sector fila(
            String idSector, String nombre, int capacidad, long vendidas, long devueltas, long aReintegrar,
            long ingresaron, BigDecimal bruta, BigDecimal devuelta, BigDecimal pendiente) {
        long vigentes = Math.max(0, vendidas - devueltas - aReintegrar);
        double ocupacion = capacidad > 0
                ? BigDecimal.valueOf(vigentes * 100.0 / capacidad).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        BigDecimal neta = bruta.subtract(devuelta).subtract(pendiente);
        return new ReporteDTO.Sector(idSector, nombre, capacidad, vendidas, devueltas, aReintegrar, vigentes,
                ingresaron, ocupacion, bruta, devuelta, pendiente, neta);
    }

    private static BigDecimal aBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return o instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) o).doubleValue());
    }

    // ------------------------------------------------- servicios externos

    /** ADMIN: cualquier evento. ORGANIZADOR: solo los suyos. Otro rol: prohibido. */
    private EventoDetalleFeignDTO cargarEventoConPermiso(String idEvento, String idUsuario, String rol) {
        boolean admin = "ADMIN".equalsIgnoreCase(rol);
        boolean organizador = "ORGANIZADOR".equalsIgnoreCase(rol);
        if (!admin && !organizador) {
            throw new ForbiddenException("Los reportes son para el organizador del evento o un ADMIN");
        }
        EventoDetalleFeignDTO evento;
        try {
            evento = eventosClient.getEvento(idEvento);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el evento (eventos)");
        }
        if (evento == null) {
            throw new ResourceNotFoundException("Evento no encontrado: " + idEvento);
        }
        if (!admin && (idUsuario == null || !idUsuario.equals(evento.getIdOrganizador()))) {
            throw new ForbiddenException("Solo el organizador del evento puede ver sus reportes");
        }
        return evento;
    }

    private List<ResumenSector> resumenEntradas(String idEvento) {
        try {
            List<ResumenSector> r = entradasClient.resumenEvento(idEvento);
            return r != null ? r : List.of();
        } catch (FeignException e) {
            // Sin entradas no se pueden informar devoluciones ni ingresos: no se entrega un reporte engañoso.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar las entradas del evento (entradas)");
        }
    }
}
