package eventos.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
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

class EventoMapperTest {

    private static final String EVENTO_ID = "e0000000-0000-4000-8000-000000000001";
    private static final String LUGAR_ID = "f0000000-0000-4000-8000-000000000001";
    private static final String SECTOR_ID = "c0000000-0000-4000-8000-000000000011";
    private static final String PARTICIPANTE_ID = "d0000000-0000-4000-8000-000000000001";

    private Evento eventoConLugar() {
        Evento e = new Evento();
        e.setId(EVENTO_ID);
        e.setIdOrganizador("org-1");
        e.setNombre("Festival X");
        e.setDescripcion("Desc");
        e.setTipo("RECITAL");
        e.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        e.setFechaFin(LocalDateTime.of(2026, 3, 15, 2, 0));
        e.setEstado("PUBLICADO");
        e.setFechaPublicacion(LocalDateTime.of(2026, 1, 20, 10, 0));
        e.setEsAltaDemanda(true);
        Lugar l = new Lugar();
        l.setId(LUGAR_ID);
        l.setNombre("Estadio");
        l.setDireccion("Av. 1");
        l.setCiudad("CABA");
        l.setCapacidadTotal(5000);
        e.setLugar(l);
        return e;
    }

    private PrecioSector precioSector(BigDecimal precio) {
        Sector s = new Sector();
        s.setId(SECTOR_ID);
        s.setNombre("Campo");
        s.setTipo("CAMPO");
        s.setCapacidad(500);
        s.setEsNumerado(false);
        PrecioSector ps = new PrecioSector();
        ps.setId("ps-1");
        ps.setSector(s);
        ps.setPrecio(precio != null ? precio : BigDecimal.valueOf(25000));
        ps.setMoneda("ARS");
        ps.setActivo(true);
        return ps;
    }

    private EventoParticipante participanteEvento(String rol, Integer orden) {
        Participante p = new Participante();
        p.setId(PARTICIPANTE_ID);
        p.setNombre("Artista");
        p.setTipo("ARTISTA");
        p.setDescripcion("Desc artista");
        EventoParticipante ep = new EventoParticipante();
        ep.setEvento(eventoConLugar());
        ep.setParticipante(p);
        ep.setRol(rol);
        ep.setOrden(orden);
        return ep;
    }

    @Test
    void toCatalogo_mapeaTodosLosCampos() {
        EventoMapper mapper = new EventoMapper();
        Evento evento = eventoConLugar();
        EventoResponseDTO dto = mapper.toCatalogo(evento, BigDecimal.valueOf(12000));

        assertNotNull(dto);
        assertEquals(EVENTO_ID, dto.getId());
        assertEquals("Festival X", dto.getNombre());
        assertEquals("RECITAL", dto.getTipo());
        assertEquals(LocalDateTime.of(2026, 3, 14, 20, 0), dto.getFechaInicio());
        assertNotNull(dto.getLugar());
        assertEquals("Estadio", dto.getLugar().getNombre());
        assertEquals("CABA", dto.getLugar().getCiudad());
        assertNull(dto.getImagenUrl());
        assertEquals(BigDecimal.valueOf(12000), dto.getPrecioDesde());
        assertEquals(false, dto.isDestacado());
        assertEquals("PUBLICADO", dto.getEstado());
        assertEquals("org-1", dto.getIdOrganizador());
        assertEquals(true, dto.isEsAltaDemanda());
    }

    @Test
    void toCatalogo_precioDesdeNulo_usaCero() {
        EventoMapper mapper = new EventoMapper();
        EventoResponseDTO dto = mapper.toCatalogo(eventoConLugar(), null);

        assertEquals(BigDecimal.ZERO, dto.getPrecioDesde());
    }

    @Test
    void toCatalogo_sinLugar_lugarResumenNulo() {
        EventoMapper mapper = new EventoMapper();
        Evento e = eventoConLugar();
        e.setLugar(null);
        EventoResponseDTO dto = mapper.toCatalogo(e, BigDecimal.TEN);

        assertNull(dto.getLugar());
    }

    @Test
    void toDetalle_mapeaTodosLosCampos() {
        EventoMapper mapper = new EventoMapper();
        Evento evento = eventoConLugar();
        List<PrecioSector> precios = List.of(precioSector(BigDecimal.valueOf(25000)));
        List<EventoParticipante> participantes = List.of(participanteEvento("PRINCIPAL", 0));

        EventoDetalleResponseDTO dto = mapper.toDetalle(evento, BigDecimal.valueOf(25000), precios, participantes);

        assertNotNull(dto);
        assertEquals(EVENTO_ID, dto.getId());
        assertEquals("Festival X", dto.getNombre());
        assertEquals("Desc", dto.getDescripcion());
        assertEquals("RECITAL", dto.getTipo());
        assertEquals(LocalDateTime.of(2026, 3, 14, 20, 0), dto.getFechaInicio());
        assertEquals(LocalDateTime.of(2026, 3, 15, 2, 0), dto.getFechaFin());
        assertEquals(LocalDateTime.of(2026, 1, 20, 10, 0), dto.getFechaPublicacion());
        assertNull(dto.getImagenUrl());
        assertEquals(BigDecimal.valueOf(25000), dto.getPrecioDesde());
        assertEquals(false, dto.isDestacado());
        assertEquals("PUBLICADO", dto.getEstado());
        assertEquals("org-1", dto.getIdOrganizador());
        assertEquals(true, dto.isEsAltaDemanda());
        assertNotNull(dto.getLugar());
        assertEquals(LUGAR_ID, dto.getLugar().getId());
        assertEquals(1, dto.getSectores().size());
        assertEquals("Campo", dto.getSectores().get(0).getNombre());
        assertEquals(BigDecimal.valueOf(25000), dto.getSectores().get(0).getPrecio());
        assertEquals(1, dto.getParticipantes().size());
        assertEquals("Artista", dto.getParticipantes().get(0).getNombre());
        assertEquals("PRINCIPAL", dto.getParticipantes().get(0).getRol());
        assertEquals(Integer.valueOf(0), dto.getParticipantes().get(0).getOrden());
    }

    @Test
    void toDetalle_preciosNulos_yParticipantesNulos() {
        EventoMapper mapper = new EventoMapper();
        EventoDetalleResponseDTO dto = mapper.toDetalle(eventoConLugar(), null, null, null);

        assertNotNull(dto);
        assertTrue(dto.getSectores().isEmpty());
        assertTrue(dto.getParticipantes().isEmpty());
        assertNotNull(dto.getLugar());
    }

    @Test
    void toParticipante_null_devuelveVacio() {
        EventoMapper mapper = new EventoMapper();
        ParticipanteResponseDTO dto = mapper.toParticipante(null);

        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getNombre());
    }

    @Test
    void toParticipante_mapeaCampos() {
        EventoMapper mapper = new EventoMapper();
        Participante p = new Participante();
        p.setId(PARTICIPANTE_ID);
        p.setNombre("Artista");
        p.setTipo("ARTISTA");
        p.setDescripcion("Desc");

        ParticipanteResponseDTO dto = mapper.toParticipante(p);

        assertEquals(PARTICIPANTE_ID, dto.getId());
        assertEquals("Artista", dto.getNombre());
        assertEquals("ARTISTA", dto.getTipo());
        assertEquals("Desc", dto.getDescripcion());
    }

    @Test
    void toParticipantesEvento_null_devuelveVacia() {
        EventoMapper mapper = new EventoMapper();
        List<ParticipanteResponseDTO> out = mapper.toParticipantesEvento(null);

        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    @Test
    void toParticipantesEvento_lista_mapeaCadaUno() {
        EventoMapper mapper = new EventoMapper();
        List<EventoParticipante> list = new ArrayList<>();
        list.add(participanteEvento("PRINCIPAL", 0));
        list.add(participanteEvento("TELONERO", 1));

        List<ParticipanteResponseDTO> out = mapper.toParticipantesEvento(list);

        assertEquals(2, out.size());
        assertEquals("PRINCIPAL", out.get(0).getRol());
        assertEquals(Integer.valueOf(0), out.get(0).getOrden());
        assertEquals("TELONERO", out.get(1).getRol());
        assertEquals(Integer.valueOf(1), out.get(1).getOrden());
    }

    @Test
    void toParticipanteEvento_null_devuelveVacio() {
        EventoMapper mapper = new EventoMapper();
        ParticipanteResponseDTO dto = mapper.toParticipanteEvento(null);

        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getRol());
        assertNull(dto.getOrden());
    }

    @Test
    void toParticipanteEvento_mapeaRolYOrden() {
        EventoMapper mapper = new EventoMapper();
        ParticipanteResponseDTO dto = mapper.toParticipanteEvento(participanteEvento("TELONERO", 5));

        assertEquals("TELONERO", dto.getRol());
        assertEquals(Integer.valueOf(5), dto.getOrden());
    }

    @Test
    void toSectoresPublic_devuelveSectores() {
        EventoMapper mapper = new EventoMapper();
        List<PrecioSector> precios = List.of(precioSector(BigDecimal.valueOf(25000)));

        List<SectorEventoDTO> out = mapper.toSectoresPublic(precios);

        assertEquals(1, out.size());
        assertEquals(SECTOR_ID, out.get(0).getId());
        assertEquals("Campo", out.get(0).getNombre());
        assertEquals(BigDecimal.valueOf(25000), out.get(0).getPrecio());
    }

    @Test
    void toSectoresPublic_null_devuelveVacia() {
        EventoMapper mapper = new EventoMapper();
        List<SectorEventoDTO> out = mapper.toSectoresPublic(null);

        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    @Test
    void toSectorDto_devuelveSector() {
        EventoMapper mapper = new EventoMapper();
        PrecioSector ps = precioSector(BigDecimal.valueOf(95000));

        SectorEventoDTO dto = mapper.toSectorDto(ps);

        assertNotNull(dto);
        assertEquals(SECTOR_ID, dto.getId());
        assertEquals(BigDecimal.valueOf(95000), dto.getPrecio());
    }

    @Test
    void toSectorFromEntity_null_devuelveConPrecioMonedaNulos() {
        EventoMapper mapper = new EventoMapper();
        SectorEventoDTO dto = mapper.toSectorFromEntity(null, BigDecimal.TEN, "ARS");

        assertNotNull(dto);
        assertNull(dto.getId());
        assertEquals(BigDecimal.TEN, dto.getPrecio());
        assertEquals("ARS", dto.getMoneda());
    }

    @Test
    void toSectorFromEntity_mapeaSector() {
        EventoMapper mapper = new EventoMapper();
        Sector s = new Sector();
        s.setId(SECTOR_ID);
        s.setNombre("Platea");
        s.setTipo("PLATEA");
        s.setCapacidad(300);
        s.setEsNumerado(true);
        s.setFilas(20);
        s.setAsientosPorFila(15);

        SectorEventoDTO dto = mapper.toSectorFromEntity(s, BigDecimal.valueOf(50000), "ARS");

        assertEquals(SECTOR_ID, dto.getId());
        assertEquals("Platea", dto.getNombre());
        assertEquals("PLATEA", dto.getTipo());
        assertEquals(Integer.valueOf(300), dto.getCapacidad());
        assertEquals(true, dto.getEsNumerado());
        assertEquals(Integer.valueOf(20), dto.getFilas());
        assertEquals(Integer.valueOf(15), dto.getAsientosPorFila());
        assertEquals(BigDecimal.valueOf(50000), dto.getPrecio());
        assertEquals("ARS", dto.getMoneda());
    }

    @Test
    void lugarResumen_yDetalle_noNulos() {
        EventoMapper mapper = new EventoMapper();
        Evento evento = eventoConLugar();

        LugarResumenDTO resumen = mapper.toCatalogo(evento, BigDecimal.TEN).getLugar();
        assertNotNull(resumen);
        assertEquals("Estadio", resumen.getNombre());
        assertEquals("CABA", resumen.getCiudad());

        EventoDetalleResponseDTO detalle = mapper.toDetalle(evento, BigDecimal.TEN, List.of(), List.of());
        LugarDetalleDTO lugarDetalle = detalle.getLugar();
        assertNotNull(lugarDetalle);
        assertEquals(LUGAR_ID, lugarDetalle.getId());
        assertEquals("Estadio", lugarDetalle.getNombre());
    }
}
