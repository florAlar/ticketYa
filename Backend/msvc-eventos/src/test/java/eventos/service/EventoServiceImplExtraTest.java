package eventos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import eventos.clients.EntradasClient;
import eventos.clients.VentasClient;
import eventos.clients.VentasClient.AbrirCuposBody;
import eventos.dto.EventoFechaRequestDTO;
import eventos.dto.EventoParticipantesRequestDTO;
import eventos.dto.EventoRequestDTO;
import eventos.dto.EventoRequestDTO.SectorInputDTO;
import eventos.dto.LugarRequestDTO;
import eventos.dto.ParticipanteRequestDTO;
import eventos.dto.PrecioSectorRequestDTO;
import eventos.dto.SectorRequestDTO;
import eventos.entity.Evento;
import eventos.entity.EventoParticipante;
import eventos.entity.Lugar;
import eventos.entity.Participante;
import eventos.entity.PrecioSector;
import eventos.entity.Sector;
import eventos.exception.BusinessConflictException;
import eventos.exception.ForbiddenException;
import eventos.exception.ResourceNotFoundException;
import eventos.mapper.EventoMapper;
import eventos.repository.EventoParticipanteRepository;
import eventos.repository.EventoRepository;
import eventos.repository.LugarRepository;
import eventos.repository.ParticipanteRepository;
import eventos.repository.PrecioSectorRepository;
import eventos.repository.SectorRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoServiceImplExtraTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private PrecioSectorRepository precioSectorRepository;

    @Mock
    private LugarRepository lugarRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParticipanteRepository participanteRepository;

    @Mock
    private EventoParticipanteRepository eventoParticipanteRepository;

    @Mock
    private EventoMapper eventoMapper;

    @Mock
    private EntradasClient entradasClient;

    @Mock
    private VentasClient ventasClient;

    @InjectMocks
    private EventoServiceImpl service;

    private static final String EVENTO_ID = "e0000000-0000-4000-8000-000000000001";
    private static final String LUGAR_ID = "f0000000-0000-4000-8000-000000000001";
    private static final String SECTOR_ID = "c0000000-0000-4000-8000-000000000011";
    private static final String ORG_ID = "b0000000-0000-4000-8000-000000000002";

    @Test
    void buscar_ceroEventos_devuelvePaginaVacia() {
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var res = service.buscar(null, null, null, null, null, null, null, "fecha", 0, 500);

        assertNotNull(res);
        assertEquals(0, res.getContent().size());
        assertEquals(0, res.getTotalElements());
    }

    @Test
    void buscar_ordenRelevancia_noLanza() {
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscar(null, null, null, null, null, null, null, "relevancia", 0, 20);
    }

    @Test
    void crear_borradorSinFechaInicio_usaProvisional() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("BORRADOR");
        req.setIdLugar(LUGAR_ID);
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        service.crear(req, ORG_ID);
    }

    @Test
    void crear_borradorSinIdLugar_creaLugarNuevo() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("BORRADOR");
        Lugar lugarNuevo = lugar("nuevo-lugar", "Por definir", "Por definir");
        when(lugarRepository.save(any(Lugar.class))).thenReturn(lugarNuevo);
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        service.crear(req, ORG_ID);
    }

    @Test
    void crear_conSectorIdExistente_usaSectorExistente() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("BORRADOR");
        req.setIdLugar(LUGAR_ID);
        SectorInputDTO si = new SectorInputDTO();
        si.setId(SECTOR_ID);
        si.setNombre("Campo");
        si.setCapacidadTotal(500);
        si.setPrecio(BigDecimal.valueOf(25000));
        req.setSectores(List.of(si));
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        Sector existente = sector(SECTOR_ID, lugar, "Campo", 500, false);
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(existente));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        service.crear(req, ORG_ID);

        verify(sectorRepository).findById(SECTOR_ID);
    }

    @Test
    void actualizar_conSectorIdExistente_usaSectorExistente() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        SectorInputDTO si = new SectorInputDTO();
        si.setId(SECTOR_ID);
        si.setNombre("Campo");
        si.setCapacidadTotal(300);
        si.setPrecio(BigDecimal.valueOf(40000));
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("X");
        req.setSectores(List.of(si));
        Sector existente = sector(SECTOR_ID, evento.getLugar(), "Campo", 300, false);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(existente));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR");

        verify(sectorRepository).findById(SECTOR_ID);
    }

    @Test
    void patchEstado_admin_cancelado_llamaReintegro() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        service.patchEstado(EVENTO_ID, "CANCELADO", ORG_ID, "ADMIN");

        verify(entradasClient).reintegroCancelacion(EVENTO_ID, "ADMIN");
    }

    @Test
    void patchEstado_org_publicado_ok() {
        Evento evento = evento("SUSPENDIDO", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(any()))
                .thenReturn(List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.valueOf(25000))));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        service.patchEstado(EVENTO_ID, "PUBLICADO", ORG_ID, "ORGANIZADOR");

        verify(ventasClient).abrirCupos(any(), any(AbrirCuposBody.class));
    }

    @Test
    void patchEstado_userIdNull_throwsForbidden() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(ForbiddenException.class,
                () -> service.patchEstado(EVENTO_ID, "SUSPENDIDO", null, "ORGANIZADOR"));
    }

    @Test
    void publicar_ventasClientFeignException_throwsBusinessConflict() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(any()))
                .thenReturn(List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.valueOf(25000))));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());
        when(ventasClient.abrirCupos(any(), any(AbrirCuposBody.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void patchEstado_cancelado_entradasClientFeignException_noLanza() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());
        when(entradasClient.reintegroCancelacion(any(), any()))
                .thenThrow(new RuntimeException("boom"));

        service.patchEstado(EVENTO_ID, "CANCELADO", ORG_ID, "ADMIN");
    }

    @Test
    void upsertPrecioSector_precioNull_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        PrecioSectorRequestDTO body = new PrecioSectorRequestDTO();
        body.setPrecio(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.upsertPrecioSector(EVENTO_ID, SECTOR_ID, body, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void obtenerPorId_findByIdWithLugarVacio_usaEventoOriginal() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID))
                .thenReturn(Optional.of(evento))
                .thenReturn(Optional.empty());
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(new eventos.dto.EventoDetalleResponseDTO());

        var res = service.obtenerPorId(EVENTO_ID);

        assertNotNull(res);
    }

    @Test
    void crearSector_capacidadNull_recalculaCapacidadCero() {
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        Sector sectorConCapacidadNull = sector(SECTOR_ID, lugar, "Campo", null, false);
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID))
                .thenReturn(List.of(sectorConCapacidadNull));
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toSectorFromEntity(any(Sector.class), any(), any()))
                .thenReturn(new eventos.dto.SectorEventoDTO());

        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre("Campo");
        body.setCapacidad(null);
        body.setTipo("CAMPO");
        body.setEsNumerado(false);

        service.crearSector(LUGAR_ID, body);

        assertEquals(Integer.valueOf(0), lugar.getCapacidadTotal());
    }

    static Sector sector(String id, Lugar lugar, String nombre, Integer cap, boolean numerado) {
        Sector s = new Sector();
        s.setId(id);
        s.setLugar(lugar);
        s.setNombre(nombre);
        s.setCapacidad(cap);
        s.setTipo(nombre.toUpperCase());
        s.setEsNumerado(numerado);
        return s;
    }

    static PrecioSector precioSector(Sector sector, BigDecimal precio) {
        PrecioSector ps = new PrecioSector();
        ps.setId("ps-1");
        ps.setSector(sector);
        ps.setPrecio(precio);
        ps.setMoneda("ARS");
        ps.setActivo(true);
        return ps;
    }

    static Lugar lugar(String id, String nombre, String ciudad) {
        Lugar l = new Lugar();
        l.setId(id);
        l.setNombre(nombre);
        l.setCiudad(ciudad);
        l.setCapacidadTotal(0);
        return l;
    }

    static Evento evento(String estado, String idOrganizador) {
        Evento e = new Evento();
        e.setId(EVENTO_ID);
        e.setIdOrganizador(idOrganizador);
        e.setNombre("Evento");
        e.setEstado(estado);
        e.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        return e;
    }
}
