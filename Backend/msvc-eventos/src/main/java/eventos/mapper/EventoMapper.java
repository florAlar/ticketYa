package eventos.mapper;

import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.LugarDetalleDTO;
import eventos.dto.LugarResumenDTO;
import eventos.dto.ParticipanteResponseDTO;
import eventos.dto.SectorEventoDTO;
import eventos.entity.Evento;
import eventos.entity.EventoParticipante;
import eventos.entity.Lugar;
import eventos.entity.Participante;
import eventos.entity.PrecioSector;
import eventos.entity.Sector;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EventoMapper {

    public EventoResponseDTO toCatalogo(Evento evento, BigDecimal precioDesde) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(evento.getId());
        dto.setNombre(evento.getNombre());
        dto.setTipo(evento.getTipo());
        dto.setFechaInicio(evento.getFechaInicio());
        dto.setLugar(toLugarResumen(evento.getLugar()));
        dto.setImagenUrl(null); // sin columna en schema
        dto.setPrecioDesde(precioDesde != null ? precioDesde : BigDecimal.ZERO);
        dto.setDestacado(false); // sin columna en schema
        dto.setEstado(evento.getEstado());
        dto.setIdOrganizador(evento.getIdOrganizador());
        dto.setEsAltaDemanda(Boolean.TRUE.equals(evento.getEsAltaDemanda()));
        return dto;
    }

    public EventoDetalleResponseDTO toDetalle(
            Evento evento,
            BigDecimal precioDesde,
            List<PrecioSector> precios,
            List<EventoParticipante> participantes) {
        EventoDetalleResponseDTO dto = new EventoDetalleResponseDTO();
        dto.setId(evento.getId());
        dto.setNombre(evento.getNombre());
        dto.setDescripcion(evento.getDescripcion());
        dto.setTipo(evento.getTipo());
        dto.setFechaInicio(evento.getFechaInicio());
        dto.setFechaFin(evento.getFechaFin());
        dto.setFechaPublicacion(evento.getFechaPublicacion());
        dto.setImagenUrl(null);
        dto.setPrecioDesde(precioDesde != null ? precioDesde : BigDecimal.ZERO);
        dto.setDestacado(false);
        dto.setEstado(evento.getEstado());
        dto.setIdOrganizador(evento.getIdOrganizador());
        dto.setEsAltaDemanda(Boolean.TRUE.equals(evento.getEsAltaDemanda()));

        List<SectorEventoDTO> sectores = toSectores(precios);
        dto.setSectores(sectores);
        dto.setLugar(toLugarDetalle(evento.getLugar(), sectores));
        dto.setParticipantes(toParticipantesEvento(participantes));
        return dto;
    }

    public ParticipanteResponseDTO toParticipante(Participante p) {
        ParticipanteResponseDTO dto = new ParticipanteResponseDTO();
        if (p == null) {
            return dto;
        }
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setTipo(p.getTipo());
        dto.setDescripcion(p.getDescripcion());
        return dto;
    }

    public ParticipanteResponseDTO toParticipanteEvento(EventoParticipante ep) {
        ParticipanteResponseDTO dto =
                toParticipante(ep != null ? ep.getParticipante() : null);
        if (ep != null) {
            dto.setRol(ep.getRol());
            dto.setOrden(ep.getOrden());
        }
        return dto;
    }

    public List<ParticipanteResponseDTO> toParticipantesEvento(List<EventoParticipante> list) {
        List<ParticipanteResponseDTO> out = new ArrayList<>();
        if (list == null) {
            return out;
        }
        for (EventoParticipante ep : list) {
            out.add(toParticipanteEvento(ep));
        }
        return out;
    }

    public List<SectorEventoDTO> toSectoresPublic(List<PrecioSector> precios) {
        return toSectores(precios);
    }

    public SectorEventoDTO toSectorDto(PrecioSector ps) {
        return toSectores(List.of(ps)).stream().findFirst().orElse(null);
    }

    public SectorEventoDTO toSectorFromEntity(Sector s, BigDecimal precio, String moneda) {
        SectorEventoDTO dto = new SectorEventoDTO();
        if (s != null) {
            dto.setId(s.getId());
            dto.setNombre(s.getNombre());
            dto.setTipo(s.getTipo());
            dto.setCapacidad(s.getCapacidad());
            dto.setEsNumerado(s.getEsNumerado());
            dto.setFilas(s.getFilas());
            dto.setAsientosPorFila(s.getAsientosPorFila());
        }
        dto.setPrecio(precio);
        dto.setMoneda(moneda);
        return dto;
    }

    private static LugarResumenDTO toLugarResumen(Lugar lugar) {
        if (lugar == null) {
            return null;
        }
        LugarResumenDTO dto = new LugarResumenDTO();
        dto.setNombre(lugar.getNombre());
        dto.setCiudad(lugar.getCiudad());
        return dto;
    }

    private static LugarDetalleDTO toLugarDetalle(Lugar lugar, List<SectorEventoDTO> sectores) {
        LugarDetalleDTO dto = new LugarDetalleDTO();
        if (lugar != null) {
            dto.setId(lugar.getId());
            dto.setNombre(lugar.getNombre());
            dto.setDireccion(lugar.getDireccion());
            dto.setCiudad(lugar.getCiudad());
            dto.setLatitud(lugar.getLatitud());
            dto.setLongitud(lugar.getLongitud());
        }
        dto.setSectores(sectores != null ? sectores : List.of());
        return dto;
    }

    private static List<SectorEventoDTO> toSectores(List<PrecioSector> precios) {
        List<SectorEventoDTO> out = new ArrayList<>();
        if (precios == null) {
            return out;
        }
        for (PrecioSector ps : precios) {
            Sector s = ps.getSector();
            SectorEventoDTO dto = new SectorEventoDTO();
            if (s != null) {
                dto.setId(s.getId());
                dto.setNombre(s.getNombre());
                dto.setTipo(s.getTipo());
                // cupo del evento (LUG-04): el propio o, si no tiene, la capacidad del sector
                dto.setCapacidad(ps.cupoEfectivo());
                dto.setEsNumerado(s.getEsNumerado());
                dto.setFilas(s.getFilas());
                dto.setAsientosPorFila(s.getAsientosPorFila());
            }
            dto.setPrecio(ps.getPrecio());
            dto.setMoneda(ps.getMoneda());
            out.add(dto);
        }
        return out;
    }
}
