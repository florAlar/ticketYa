package eventos.mapper;

import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.LugarDetalleDTO;
import eventos.dto.LugarResumenDTO;
import eventos.dto.SectorEventoDTO;
import eventos.entity.Evento;
import eventos.entity.Lugar;
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
            Evento evento, BigDecimal precioDesde, List<PrecioSector> precios) {
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
        return dto;
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
                dto.setCapacidad(s.getCapacidad());
                dto.setEsNumerado(s.getEsNumerado());
            }
            dto.setPrecio(ps.getPrecio());
            dto.setMoneda(ps.getMoneda());
            out.add(dto);
        }
        return out;
    }
}
