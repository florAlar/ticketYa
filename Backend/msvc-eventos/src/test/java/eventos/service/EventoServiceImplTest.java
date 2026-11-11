package eventos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import eventos.clients.EntradasClient;
import eventos.clients.VentasClient;
import eventos.clients.VentasClient.AbrirCuposBody;
import eventos.dto.EventoFechaRequestDTO;
import eventos.dto.EventoParticipantesRequestDTO;
import eventos.dto.EventoRequestDTO;
import eventos.dto.EventoRequestDTO.SectorInputDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.LugarRequestDTO;
import eventos.dto.LugarResponseDTO;
import eventos.dto.ParticipanteRequestDTO;
import eventos.dto.ParticipanteResponseDTO;
import eventos.dto.PrecioSectorRequestDTO;
import eventos.dto.SectorEventoDTO;
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
class EventoServiceImplTest {

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

    @BeforeEach
    void setUp() {
        reset(eventoRepository, precioSectorRepository, lugarRepository, sectorRepository,
                participanteRepository, eventoParticipanteRepository,
                eventoMapper, entradasClient, ventasClient);
        when(eventoRepository.findByIdWithLugar(any())).thenReturn(Optional.empty());
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(eventoRepository.findByIdOrganizadorOrderByFechaInicioDesc(any()))
                .thenReturn(List.of());
        when(lugarRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());
        when(lugarRepository.findByCiudadContainingIgnoreCaseOrderByNombreAsc(any()))
                .thenReturn(List.of());
        when(sectorRepository.findByLugarIdOrderByNombreAsc(any())).thenReturn(List.of());
        when(precioSectorRepository.minPrecioByEventoIds(anyList())).thenReturn(List.of());
        when(precioSectorRepository.minPrecioByEventoId(any())).thenReturn(null);
        when(precioSectorRepository.findActivosByEventoId(any())).thenReturn(List.of());
        when(precioSectorRepository.findByEventoIdAndSectorId(any(), any()))
                .thenReturn(Optional.empty());
        when(precioSectorRepository.save(any(PrecioSector.class))).thenAnswer(i -> i.getArgument(0));
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoParticipanteRepository.findByEventoIdOrdered(any())).thenReturn(List.of());
        when(eventoParticipanteRepository.save(any(EventoParticipante.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(participanteRepository.findById(any())).thenReturn(Optional.empty());
        when(participanteRepository.buscar(any(), any())).thenReturn(List.of());
        when(participanteRepository.save(any(Participante.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toSectorFromEntity(any(Sector.class), any(), any()))
                .thenReturn(new SectorEventoDTO());
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList()))
                .thenReturn(new eventos.dto.EventoDetalleResponseDTO());
        when(eventoMapper.toCatalogo(any(Evento.class), any(BigDecimal.class)))
                .thenReturn(new EventoResponseDTO());
        when(ventasClient.abrirCupos(any(), any(AbrirCuposBody.class))).thenReturn(null);
    }

    // ---------- buscar ----------

    @Test
    void buscar_catalogoDefault_devuelvePagina() {
        Evento e = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(e)));
        when(eventoMapper.toCatalogo(e, null)).thenReturn(dto(e));

        var res = service.buscar(null, null, null, null, null, null, null, "fecha", 0, 20);

        assertNotNull(res);
        assertEquals(1, res.getContent().size());
    }

    @Test
    void buscar_estadoTodos_ignoraEstado() {
        Evento e = evento("CANCELADO", ORG_ID);
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(e)));
        when(eventoMapper.toCatalogo(e, null)).thenReturn(dto(e));

        service.buscar(null, null, null, "TODOS", null, null, null, "fecha", 0, 20);

        verify(eventoRepository).buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void buscar_pageNegativo_ySizeCero_usaSeguros() {
        Evento e = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(e)));
        when(eventoMapper.toCatalogo(e, null)).thenReturn(dto(e));

        service.buscar(null, null, null, null, null, null, null, "fecha", -5, 0);

        verify(eventoRepository).buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void buscar_ordenPrecio_ordenaContenido() {
        Evento e1 = evento("PUBLICADO", ORG_ID);
        Evento e2 = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.buscar(any(), any(), any(), any(),
                anyBoolean(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(e1, e2)));
        EventoResponseDTO d1 = new EventoResponseDTO();
        d1.setPrecioDesde(BigDecimal.valueOf(100));
        EventoResponseDTO d2 = new EventoResponseDTO();
        d2.setPrecioDesde(BigDecimal.valueOf(50));
        when(eventoMapper.toCatalogo(e1, null)).thenReturn(d1);
        when(eventoMapper.toCatalogo(e2, null)).thenReturn(d2);

        var res = service.buscar(null, null, null, null, null, null, null, "precio", 0, 20);

        assertEquals(BigDecimal.valueOf(50), res.getContent().get(0).getPrecioDesde());
        assertEquals(BigDecimal.valueOf(100), res.getContent().get(1).getPrecioDesde());
    }

    // ---------- obtenerPorId ----------

    @Test
    void obtenerPorId_ok() {
        Evento e = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(e));
        when(eventoMapper.toDetalle(eq(e), any(), anyList(), anyList())).thenReturn(detalle(e));

        var res = service.obtenerPorId(EVENTO_ID);

        assertNotNull(res);
        assertEquals(EVENTO_ID, res.getId());
    }

    @Test
    void obtenerPorId_noEncontrado_throws() {
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(EVENTO_ID));
    }

    // ---------- listarDeOrganizador ----------

    @Test
    void listarDeOrganizador_ok() {
        Evento e = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdOrganizadorOrderByFechaInicioDesc(ORG_ID))
                .thenReturn(List.of(e));

        var res = service.listarDeOrganizador(ORG_ID);

        assertEquals(1, res.size());
    }

    // ---------- crear ----------

    @Test
    void crear_borradorConIdLugarExistente_ok() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("BORRADOR");
        req.setIdLugar(LUGAR_ID);
        req.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        when(sectorRepository.findById(any())).thenReturn(Optional.empty());

        service.crear(req, ORG_ID);

        verify(eventoRepository).save(any(Evento.class));
        verify(ventasClient, times(0)).abrirCupos(any(), any());
    }

    @Test
    void crear_publicarSinFecha_throws() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("PUBLICADO");
        req.setIdLugar(LUGAR_ID);

        assertThrows(IllegalArgumentException.class, () -> service.crear(req, ORG_ID));
    }

    @Test
    void crear_publicarSinSectores_throws() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("PUBLICADO");
        req.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        req.setIdLugar(LUGAR_ID);
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));

        assertThrows(IllegalArgumentException.class, () -> service.crear(req, ORG_ID));
    }

    @Test
    void crear_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(null, ORG_ID));
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(lugarRepository.findById(any())).thenReturn(Optional.empty());
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Nuevo Nombre");

        service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR");

        assertEquals("Nuevo Nombre", evento.getNombre());
    }

    @Test
    void actualizar_estadoCancelado_throws() {
        Evento evento = evento("CANCELADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("X");

        assertThrows(BusinessConflictException.class,
                () -> service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void actualizar_noEsDueño_throwsForbidden() {
        Evento evento = evento("PUBLICADO", "otro-org");
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("X");

        assertThrows(ForbiddenException.class,
                () -> service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR"));
    }

    // ---------- eliminarBorrador ----------

    @Test
    void eliminarBorrador_borrador_ok() {
        Evento evento = evento("BORRADOR", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findByEventoId(EVENTO_ID)).thenReturn(List.of());

        service.eliminarBorrador(EVENTO_ID, ORG_ID, "ORGANIZADOR");

        verify(eventoRepository).delete(evento);
    }

    @Test
    void eliminarBorrador_noEsBorrador_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(BusinessConflictException.class,
                () -> service.eliminarBorrador(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    // ---------- publicar ----------

    @Test
    void publicar_ok() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(any()))
                .thenReturn(List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.valueOf(25000))));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR");

        verify(ventasClient, times(1)).abrirCupos(any(String.class), any(AbrirCuposBody.class));
    }

    @Test
    void publicar_estadoCancelado_throws() {
        Evento evento = evento("CANCELADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void publicar_sinNombre_throws() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setNombre(null);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(EVENTO_ID))
                .thenReturn(List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.valueOf(25000))));

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void publicar_sinFechaInicio_throws() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setNombre("Ev");
        evento.setFechaInicio(null);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(EVENTO_ID))
                .thenReturn(List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.valueOf(25000))));

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void publicar_lugarNombreBorrador_throws() {
        Evento evento = evento("BORRADOR", ORG_ID);
        Lugar l = lugar(LUGAR_ID, "Por definir", "CABA");
        evento.setLugar(l);
        evento.setNombre("Ev");
        evento.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(EVENTO_ID)).thenReturn(List.of());

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void publicar_sinPreciosActivos_throws() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setNombre("Ev");
        evento.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(EVENTO_ID)).thenReturn(List.of());

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void publicar_precioNegativo_throws() {
        Sector sec = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Campo", 500, false);
        PrecioSector ps = precioSector(sec, BigDecimal.valueOf(-10));
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setNombre("Ev");
        evento.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(EVENTO_ID)).thenReturn(List.of(ps));

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void publicar_cupoCero_throws() {
        Sector sec = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Campo", 0, false);
        PrecioSector ps = precioSector(sec, BigDecimal.valueOf(100));
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setNombre("Ev");
        evento.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(EVENTO_ID)).thenReturn(List.of(ps));

        assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
    }

    // ---------- patchEstado ----------

    @Test
    void patchEstado_admin_suspendido_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        service.patchEstado(EVENTO_ID, "SUSPENDIDO", ORG_ID, "ADMIN");

        assertEquals("SUSPENDIDO", evento.getEstado());
    }

    @Test
    void patchEstado_org_suspendidoPropio_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        service.patchEstado(EVENTO_ID, "SUSPENDIDO", ORG_ID, "ORGANIZADOR");

        assertEquals("SUSPENDIDO", evento.getEstado());
    }

    @Test
    void patchEstado_org_canceladoPropio_forbidden() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(ForbiddenException.class,
                () -> service.patchEstado(EVENTO_ID, "CANCELADO", ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void patchEstado_admin_publicado_llamaDoPublicar() {
        Evento evento = evento("SUSPENDIDO", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(precioSectorRepository.findActivosByEventoId(any()))
                .thenReturn(List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.valueOf(25000))));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        service.patchEstado(EVENTO_ID, "PUBLICADO", ORG_ID, "ADMIN");

        verify(ventasClient, times(1)).abrirCupos(any(String.class), any(AbrirCuposBody.class));
    }

    @Test
    void patchEstado_estadoNull_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(IllegalArgumentException.class,
                () -> service.patchEstado(EVENTO_ID, null, ORG_ID, "ADMIN"));
    }

    @Test
    void patchEstado_estadoInvalido_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(IllegalArgumentException.class,
                () -> service.patchEstado(EVENTO_ID, "INEXISTENTE", ORG_ID, "ADMIN"));
    }

    @Test
    void patchEstado_borradorACancelado_throws() {
        Evento evento = evento("BORRADOR", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(BusinessConflictException.class,
                () -> service.patchEstado(EVENTO_ID, "CANCELADO", ORG_ID, "ADMIN"));
    }

    // ---------- patchFecha ----------

    @Test
    void patchFecha_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        EventoFechaRequestDTO body = new EventoFechaRequestDTO();
        body.setFechaInicio(LocalDateTime.of(2026, 4, 10, 21, 0));
        body.setFechaFin(LocalDateTime.of(2026, 4, 11, 2, 0));

        service.patchFecha(EVENTO_ID, body, ORG_ID, "ORGANIZADOR");

        assertEquals(LocalDateTime.of(2026, 4, 10, 21, 0), evento.getFechaInicio());
    }

    @Test
    void patchFecha_estadoCancelado_throws() {
        Evento evento = evento("CANCELADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        EventoFechaRequestDTO body = new EventoFechaRequestDTO();
        body.setFechaInicio(LocalDateTime.of(2026, 4, 10, 21, 0));

        assertThrows(BusinessConflictException.class,
                () -> service.patchFecha(EVENTO_ID, body, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void patchFecha_bodyNull_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(IllegalArgumentException.class,
                () -> service.patchFecha(EVENTO_ID, null, ORG_ID, "ORGANIZADOR"));
    }

    // ---------- listarSectoresEvento ----------

    @Test
    void listarSectoresEvento_ok() {
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento()));
        when(precioSectorRepository.findAllByEventoId(EVENTO_ID)).thenReturn(List.of());
        when(eventoMapper.toSectoresPublic(List.of())).thenReturn(List.of());

        var res = service.listarSectoresEvento(EVENTO_ID);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    // ---------- upsertPrecioSector ----------

    @Test
    void upsertPrecioSector_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        Sector sector = sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(sector));
        when(precioSectorRepository.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_ID))
                .thenReturn(Optional.of(precioSector(sector, BigDecimal.valueOf(25000))));
        when(precioSectorRepository.save(any(PrecioSector.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toSectorDto(any(PrecioSector.class))).thenReturn(new SectorEventoDTO());

        PrecioSectorRequestDTO body = new PrecioSectorRequestDTO();
        body.setPrecio(BigDecimal.valueOf(30000));

        service.upsertPrecioSector(EVENTO_ID, SECTOR_ID, body, ORG_ID, "ORGANIZADOR");

        verify(precioSectorRepository).save(any(PrecioSector.class));
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
    void upsertPrecioSector_sectorNoPerteneceAlLugar_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        Sector sectorOtro = sector("sec-otro", lugar("f002", "Otro", "Otra"), "X", 100, false);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(sectorOtro));

        PrecioSectorRequestDTO body = new PrecioSectorRequestDTO();
        body.setPrecio(BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class,
                () -> service.upsertPrecioSector(EVENTO_ID, SECTOR_ID, body, ORG_ID, "ORGANIZADOR"));
    }

    // ---------- listarLugares ----------

    @Test
    void listarLugares_sinCiudad() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(l));
        when(eventoMapper.toSectorFromEntity(any(), any(), any())).thenReturn(new SectorEventoDTO());

        var res = service.listarLugares(null);

        assertEquals(1, res.size());
    }

    @Test
    void listarLugares_conCiudad() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findByCiudadContainingIgnoreCaseOrderByNombreAsc("CABA"))
                .thenReturn(List.of(l));
        when(eventoMapper.toSectorFromEntity(any(), any(), any())).thenReturn(new SectorEventoDTO());

        var res = service.listarLugares("CABA");

        assertEquals(1, res.size());
    }

    // ---------- obtenerLugar ----------

    @Test
    void obtenerLugar_ok() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of());

        var res = service.obtenerLugar(LUGAR_ID);

        assertNotNull(res);
        assertEquals(LUGAR_ID, res.getId());
    }

    // ---------- crearLugar ----------

    @Test
    void crearLugar_bodyNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.crearLugar(null));
    }

    @Test
    void crearLugar_ok() {
        LugarRequestDTO body = new LugarRequestDTO();
        body.setNombre("Nuevo Lugar");
        body.setCiudad("Rosario");
        Lugar lugar = lugar("nuevo", "Nuevo Lugar", "Rosario");
        when(lugarRepository.save(any(Lugar.class))).thenReturn(lugar);
        when(sectorRepository.findByLugarIdOrderByNombreAsc("nuevo")).thenReturn(List.of());

        var res = service.crearLugar(body);

        assertNotNull(res);
        assertEquals("Nuevo Lugar", res.getNombre());
    }

    // ---------- actualizarLugar ----------

    @Test
    void actualizarLugar_bodyNull_throws() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "X", "Y")));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarLugar(LUGAR_ID, null));
    }

    @Test
    void actualizarLugar_ok() {
        Lugar l = lugar(LUGAR_ID, "Viejo", "Vieja");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of());

        LugarRequestDTO body = new LugarRequestDTO();
        body.setNombre("Nuevo");
        body.setCiudad("Nueva");

        var res = service.actualizarLugar(LUGAR_ID, body);

        assertEquals("Nuevo", l.getNombre());
        assertEquals("Nueva", l.getCiudad());
    }

    // ---------- coordenadas del lugar (LUG-02) ----------

    private static java.math.BigDecimal bd(String s) {
        return new java.math.BigDecimal(s);
    }

    @Test
    void crearLugar_conCoordenadas_laGuardaConSeisDecimales_yLaDevuelve() {
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(any())).thenReturn(List.of());
        LugarRequestDTO body = new LugarRequestDTO();
        body.setNombre("Estadio");
        body.setCiudad("Buenos Aires");
        body.setLatitud(bd("-34.5453"));
        body.setLongitud(bd("-58.4497"));

        var res = service.crearLugar(body);

        assertEquals(0, bd("-34.545300").compareTo(res.getLatitud()));
        assertEquals(0, bd("-58.449700").compareTo(res.getLongitud()));
        assertEquals(6, res.getLatitud().scale());
    }

    @Test
    void crearLugar_sinCoordenadas_quedanNulas() {
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(any())).thenReturn(List.of());
        LugarRequestDTO body = new LugarRequestDTO();
        body.setNombre("Estadio");
        body.setCiudad("Rosario");

        var res = service.crearLugar(body);

        assertNull(res.getLatitud());
        assertNull(res.getLongitud());
    }

    @Test
    void crearLugar_coordenadasIncompletasOFueraDeRango_son400_yNoSeGuarda() {
        LugarRequestDTO soloLat = new LugarRequestDTO();
        soloLat.setNombre("X");
        soloLat.setCiudad("Y");
        soloLat.setLatitud(bd("10"));
        assertThrows(IllegalArgumentException.class, () -> service.crearLugar(soloLat));

        LugarRequestDTO soloLng = new LugarRequestDTO();
        soloLng.setNombre("X");
        soloLng.setCiudad("Y");
        soloLng.setLongitud(bd("10"));
        assertThrows(IllegalArgumentException.class, () -> service.crearLugar(soloLng));

        LugarRequestDTO fuera = new LugarRequestDTO();
        fuera.setNombre("X");
        fuera.setCiudad("Y");
        fuera.setLatitud(bd("91"));
        fuera.setLongitud(bd("0"));
        assertThrows(IllegalArgumentException.class, () -> service.crearLugar(fuera));

        verify(lugarRepository, times(0)).save(any(Lugar.class));
    }

    @Test
    void actualizarLugar_sinCoordenadasEnElPedido_conservaLasExistentes_yConCoordenadasLasCambia() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "Buenos Aires");
        l.setLatitud(bd("-34.545300"));
        l.setLongitud(bd("-58.449700"));
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of());

        LugarRequestDTO soloNombre = new LugarRequestDTO();
        soloNombre.setNombre("Estadio Renombrado");
        service.actualizarLugar(LUGAR_ID, soloNombre);
        assertEquals(0, bd("-34.545300").compareTo(l.getLatitud()));

        LugarRequestDTO nuevas = new LugarRequestDTO();
        nuevas.setLatitud(bd("-31.4"));
        nuevas.setLongitud(bd("-64.18"));
        service.actualizarLugar(LUGAR_ID, nuevas);
        assertEquals(0, bd("-31.400000").compareTo(l.getLatitud()));
        assertEquals(0, bd("-64.180000").compareTo(l.getLongitud()));
    }

    @Test
    void actualizarLugar_quitarCoordenadas_lasBorra_ySiSeCombinaConValores_es400() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "Buenos Aires");
        l.setLatitud(bd("-34.545300"));
        l.setLongitud(bd("-58.449700"));
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of());

        LugarRequestDTO combinado = new LugarRequestDTO();
        combinado.setQuitarCoordenadas(true);
        combinado.setLatitud(bd("1"));
        combinado.setLongitud(bd("1"));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarLugar(LUGAR_ID, combinado));

        LugarRequestDTO quitar = new LugarRequestDTO();
        quitar.setQuitarCoordenadas(true);
        service.actualizarLugar(LUGAR_ID, quitar);

        assertNull(l.getLatitud());
        assertNull(l.getLongitud());
    }

    // ---------- eliminarLugar (LUG-01) ----------

    @Test
    void eliminarLugar_sinEventos_borraSectoresYLugar() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "Buenos Aires");
        Sector s1 = sector(SECTOR_ID, l, "Campo", 100, false);
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        when(eventoRepository.existsByLugarId(LUGAR_ID)).thenReturn(false);
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of(s1));

        service.eliminarLugar(LUGAR_ID);

        verify(sectorRepository).deleteAll(List.of(s1));
        verify(lugarRepository).delete(l);
    }

    @Test
    void eliminarLugar_conEventos_es409_yNoBorraNada() {
        Lugar l = lugar(LUGAR_ID, "Estadio", "Buenos Aires");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        when(eventoRepository.existsByLugarId(LUGAR_ID)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> service.eliminarLugar(LUGAR_ID));

        verify(lugarRepository, times(0)).delete(any(Lugar.class));
        verify(sectorRepository, times(0)).deleteAll(any());
    }

    @Test
    void eliminarLugar_inexistente_es404() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarLugar(LUGAR_ID));
    }

    // ---------- crearSector ----------

    @Test
    void crearSector_bodyNull_throws() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "X", "Y")));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearSector(LUGAR_ID, null));
    }

    @Test
    void crearSector_nombreNull_throws() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "X", "Y")));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.crearSector(LUGAR_ID, body));
    }

    @Test
    void crearSector_ok() {
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        Sector s = sector(SECTOR_ID, lugar, "Campo", 500, false);
        when(sectorRepository.save(any(Sector.class))).thenReturn(s);
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of());

        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre("Campo");
        body.setCapacidad(500);
        body.setTipo("CAMPO");
        body.setEsNumerado(false);

        var res = service.crearSector(LUGAR_ID, body);

        assertNotNull(res);
    }

    // ---------- actualizarSector ----------

    @Test
    void actualizarSector_noEncontrado_throws() {
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizarSector(SECTOR_ID, new SectorRequestDTO()));
    }

    @Test
    void actualizarSector_bodyNull_throws() {
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(sector(SECTOR_ID, null, "X", 100, false)));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarSector(SECTOR_ID, null));
    }

    @Test
    void actualizarSector_ok() {
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        Sector s = sector(SECTOR_ID, lugar, "Campo", 500, false);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));
        when(sectorRepository.findByLugarIdOrderByNombreAsc(LUGAR_ID)).thenReturn(List.of());

        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre("Platea");
        body.setCapacidad(999); // se ignora: en un sector numerado la capacidad se deriva del layout
        body.setTipo("PLATEA");
        body.setEsNumerado(true);
        body.setFilas(10);
        body.setAsientosPorFila(30);

        service.actualizarSector(SECTOR_ID, body);

        assertEquals("Platea", s.getNombre());
        assertEquals(Integer.valueOf(300), s.getCapacidad());
        assertEquals(Integer.valueOf(10), s.getFilas());
        assertEquals(Integer.valueOf(30), s.getAsientosPorFila());
        assertEquals("PLATEA", s.getTipo());
        assertEquals(true, s.getEsNumerado());
    }

    @Test
    void actualizarSector_numeradoSinLayout_throws() {
        Sector s = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Campo", 500, false);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setEsNumerado(true);

        assertThrows(IllegalArgumentException.class, () -> service.actualizarSector(SECTOR_ID, body));
    }

    @Test
    void actualizarSector_numeradoConLayoutExistente_mantieneLayout() {
        Sector s = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Platea", 300, true);
        s.setFilas(10);
        s.setAsientosPorFila(30);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setFilas(20);

        service.actualizarSector(SECTOR_ID, body);

        assertEquals(Integer.valueOf(20), s.getFilas());
        assertEquals(Integer.valueOf(30), s.getAsientosPorFila());
        assertEquals(Integer.valueOf(600), s.getCapacidad());
    }

    @Test
    void actualizarSector_aGeneral_limpiaLayout() {
        Sector s = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Platea", 300, true);
        s.setFilas(10);
        s.setAsientosPorFila(30);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setEsNumerado(false);
        body.setCapacidad(400);

        service.actualizarSector(SECTOR_ID, body);

        assertEquals(false, s.getEsNumerado());
        assertNull(s.getFilas());
        assertNull(s.getAsientosPorFila());
        assertEquals(Integer.valueOf(400), s.getCapacidad());
    }

    // ---------- crearSector numerado ----------

    @Test
    void crearSector_numeradoDerivaCapacidadDelLayout() {
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre("Platea");
        body.setEsNumerado(true);
        body.setFilas(20);
        body.setAsientosPorFila(15);

        service.crearSector(LUGAR_ID, body);

        var captor = org.mockito.ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(captor.capture());
        assertEquals(Integer.valueOf(300), captor.getValue().getCapacidad());
        assertEquals(Integer.valueOf(20), captor.getValue().getFilas());
        assertEquals(Integer.valueOf(15), captor.getValue().getAsientosPorFila());
    }

    @Test
    void crearSector_numeradoSinLayout_throws() {
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre("Platea");
        body.setEsNumerado(true);
        body.setCapacidad(300);

        assertThrows(IllegalArgumentException.class, () -> service.crearSector(LUGAR_ID, body));
    }

    @Test
    void crearSector_numeradoLayoutFueraDeRango_throws() {
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar));
        SectorRequestDTO body = new SectorRequestDTO();
        body.setNombre("Platea");
        body.setEsNumerado(true);
        body.setFilas(201);
        body.setAsientosPorFila(10);

        assertThrows(IllegalArgumentException.class, () -> service.crearSector(LUGAR_ID, body));
    }

    // ---------- evento con sector numerado ----------

    @Test
    void crear_publicarSectorNumeradoSinLayout_throws() {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("PUBLICADO");
        req.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        req.setIdLugar(LUGAR_ID);
        SectorInputDTO si = new SectorInputDTO();
        si.setNombre("Platea");
        si.setCapacidadTotal(300);
        si.setEsNumerado(true);
        si.setPrecio(BigDecimal.valueOf(25000));
        req.setSectores(List.of(si));
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "Estadio", "CABA")));

        assertThrows(IllegalArgumentException.class, () -> service.crear(req, ORG_ID));
    }

    // ---------- LUG-04: el evento elige un lugar; los sectores los define el lugar ----------

    private static final String POR_DEFINIR_ID = "c0000000-0000-4000-8000-0000000000ff";

    private SectorInputDTO elegir(String idSector, int precio) {
        SectorInputDTO in = new SectorInputDTO();
        in.setId(idSector);
        in.setPrecio(BigDecimal.valueOf(precio));
        return in;
    }

    private EventoRequestDTO borradorEn(String idLugar, SectorInputDTO... sectores) {
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Evento");
        req.setEstado("BORRADOR");
        req.setIdLugar(idLugar);
        if (sectores.length > 0) {
            req.setSectores(List.of(sectores));
        }
        return req;
    }

    private Lugar lugarConSector(Sector[] salida, String nombreSector, int capacidad, boolean numerado) {
        Lugar l = lugar(LUGAR_ID, "Estadio", "CABA");
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(l));
        salida[0] = sector(SECTOR_ID, l, nombreSector, capacidad, numerado);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(salida[0]));
        return l;
    }

    private PrecioSector ultimoPrecioGuardado() {
        var captor = org.mockito.ArgumentCaptor.forClass(PrecioSector.class);
        verify(precioSectorRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void crear_sinLugar_borrador_usaElLugarPorDefinirCompartido_yLoCreaUnaSolaVez() {
        when(lugarRepository.findById(POR_DEFINIR_ID)).thenReturn(Optional.empty());

        service.crear(borradorEn(null), ORG_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(Lugar.class);
        verify(lugarRepository).save(captor.capture());
        assertEquals(POR_DEFINIR_ID, captor.getValue().getId());
        assertEquals("Por definir", captor.getValue().getNombre());

        // el segundo borrador reutiliza el mismo lugar: no se vuelve a crear
        org.mockito.Mockito.clearInvocations(lugarRepository);
        when(lugarRepository.findById(POR_DEFINIR_ID)).thenReturn(Optional.of(captor.getValue()));
        service.crear(borradorEn(""), ORG_ID);
        verify(lugarRepository, times(0)).save(any(Lugar.class));
    }

    @Test
    void crear_sinLugar_publicar_throws() {
        EventoRequestDTO req = borradorEn(null);
        req.setEstado("PUBLICADO");
        req.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.crear(req, ORG_ID));
        assertTrue(e.getMessage().contains("idLugar"));
    }

    @Test
    void crear_sinLugar_conSectores_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(borradorEn(null, elegir(SECTOR_ID, 100)), ORG_ID));
    }

    @Test
    void crear_elLugarPorDefinirNoSeEligeComoLugarReal() {
        // pasarlo como idLugar equivale a "sin lugar": un borrador, nunca un lugar elegible
        when(lugarRepository.findById(POR_DEFINIR_ID)).thenReturn(Optional.empty());
        EventoRequestDTO req = borradorEn(POR_DEFINIR_ID);
        req.setEstado("PUBLICADO");
        req.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));

        assertThrows(IllegalArgumentException.class, () -> service.crear(req, ORG_ID));
    }

    @Test
    void crear_sectorSinId_throws_yNoSeCreanSectoresDesdeElEvento() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "Estadio", "CABA")));
        SectorInputDTO nuevo = new SectorInputDTO();
        nuevo.setNombre("Campo nuevo");
        nuevo.setCapacidadTotal(500);
        nuevo.setPrecio(BigDecimal.valueOf(1000));

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.crear(borradorEn(LUGAR_ID, nuevo), ORG_ID));

        assertTrue(e.getMessage().contains("sectores[].id"));
        verify(sectorRepository, times(0)).save(any(Sector.class));
    }

    @Test
    void crear_sectorRepetido_throws() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "Estadio", "CABA")));

        assertThrows(IllegalArgumentException.class,
                () -> service.crear(borradorEn(LUGAR_ID, elegir(SECTOR_ID, 1), elegir(SECTOR_ID, 2)), ORG_ID));
    }

    @Test
    void crear_sectorInexistente_404() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "Estadio", "CABA")));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.crear(borradorEn(LUGAR_ID, elegir(SECTOR_ID, 1000)), ORG_ID));
    }

    @Test
    void crear_sectorDeOtroLugar_throws() {
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(lugar(LUGAR_ID, "Estadio", "CABA")));
        when(sectorRepository.findById(SECTOR_ID))
                .thenReturn(Optional.of(sector(SECTOR_ID, lugar("otro", "Otro", "X"), "Campo", 500, false)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.crear(borradorEn(LUGAR_ID, elegir(SECTOR_ID, 1000)), ORG_ID));
        assertTrue(e.getMessage().contains("no pertenece al lugar"));
    }

    @Test
    void crear_elEventoNoPuedeCambiarNombreTipoNiLayoutDelSector() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Platea", 200, true);
        s[0].setFilas(10);
        s[0].setAsientosPorFila(20);

        SectorInputDTO otroNombre = elegir(SECTOR_ID, 1000);
        otroNombre.setNombre("Otro nombre");
        SectorInputDTO otroTipo = elegir(SECTOR_ID, 1000);
        otroTipo.setTipo("VIP");
        SectorInputDTO dejaDeSerNumerado = elegir(SECTOR_ID, 1000);
        dejaDeSerNumerado.setEsNumerado(false);
        SectorInputDTO otroLayout = elegir(SECTOR_ID, 1000);
        otroLayout.setFilas(5);

        for (SectorInputDTO in : List.of(otroNombre, otroTipo, dejaDeSerNumerado, otroLayout)) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.crear(borradorEn(LUGAR_ID, in), ORG_ID));
            assertTrue(e.getMessage().contains("lo define el lugar"), e.getMessage());
        }
        verify(sectorRepository, times(0)).save(any(Sector.class));
    }

    @Test
    void crear_datosDelSectorQueCoinciden_seAceptan() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Campo", 500, false);
        SectorInputDTO in = elegir(SECTOR_ID, 25000);
        in.setNombre("campo");
        in.setTipo("CAMPO");
        in.setEsNumerado(false);

        service.crear(borradorEn(LUGAR_ID, in), ORG_ID);

        assertEquals(0, BigDecimal.valueOf(25000).compareTo(ultimoPrecioGuardado().getPrecio()));
    }

    @Test
    void crear_sectorGeneral_cupoMenorALaCapacidad_seGuardaEnElPrecioDelEvento_yElSectorNoSeTocaa() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Campo", 500, false);
        SectorInputDTO in = elegir(SECTOR_ID, 25000);
        in.setCapacidadTotal(120);

        service.crear(borradorEn(LUGAR_ID, in), ORG_ID);

        PrecioSector ps = ultimoPrecioGuardado();
        assertEquals(Integer.valueOf(120), ps.getCupo());
        assertEquals(Integer.valueOf(120), ps.cupoEfectivo());
        assertEquals(Integer.valueOf(500), s[0].getCapacidad(), "la capacidad del sector la define el lugar");
        verify(sectorRepository, times(0)).save(any(Sector.class));
    }

    @Test
    void crear_sectorGeneral_sinCupoOCupoIgualALaCapacidad_vendeTodoElSector() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Campo", 500, false);
        SectorInputDTO sinCupo = elegir(SECTOR_ID, 25000);
        service.crear(borradorEn(LUGAR_ID, sinCupo), ORG_ID);
        assertNull(ultimoPrecioGuardado().getCupo());
        assertEquals(Integer.valueOf(500), ultimoPrecioGuardado().cupoEfectivo());

        org.mockito.Mockito.clearInvocations(precioSectorRepository);
        SectorInputDTO igual = elegir(SECTOR_ID, 25000);
        igual.setCapacidadTotal(500);
        service.crear(borradorEn(LUGAR_ID, igual), ORG_ID);
        assertNull(ultimoPrecioGuardado().getCupo());
    }

    @Test
    void crear_sectorGeneral_cupoFueraDeRango_throws() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Campo", 500, false);

        for (int cupo : new int[] {0, -5, 501}) {
            SectorInputDTO in = elegir(SECTOR_ID, 25000);
            in.setCapacidadTotal(cupo);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.crear(borradorEn(LUGAR_ID, in), ORG_ID), "cupo " + cupo);
            assertTrue(e.getMessage().contains("entre 1 y 500"), e.getMessage());
        }
    }

    @Test
    void crear_sectorNumerado_seVendeCompleto_yNoAdmiteUnCupoMenor() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Platea", 200, true);

        SectorInputDTO completo = elegir(SECTOR_ID, 25000);
        completo.setCapacidadTotal(200);
        service.crear(borradorEn(LUGAR_ID, completo), ORG_ID);
        assertNull(ultimoPrecioGuardado().getCupo());
        assertEquals(Integer.valueOf(200), ultimoPrecioGuardado().cupoEfectivo());

        SectorInputDTO parcial = elegir(SECTOR_ID, 25000);
        parcial.setCapacidadTotal(100);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.crear(borradorEn(LUGAR_ID, parcial), ORG_ID));
        assertTrue(e.getMessage().contains("se vende completo"));
    }

    @Test
    void crear_publicarOk_conSectoresDelLugar_abreLosCuposConElCupoDelEvento() {
        Sector[] s = new Sector[1];
        lugarConSector(s, "Campo", 500, false);
        SectorInputDTO in = elegir(SECTOR_ID, 25000);
        in.setCapacidadTotal(120);
        EventoRequestDTO req = borradorEn(LUGAR_ID, in);
        req.setEstado("PUBLICADO");
        req.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        PrecioSector ps = precioSector(s[0], BigDecimal.valueOf(25000));
        ps.setCupo(120);
        when(precioSectorRepository.findActivosByEventoId(any())).thenReturn(List.of(ps));
        when(eventoMapper.toDetalle(any(), any(), anyList(), anyList())).thenReturn(detalle(new Evento()));

        service.crear(req, ORG_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(AbrirCuposBody.class);
        verify(ventasClient, times(1)).abrirCupos(any(String.class), captor.capture());
        assertEquals(1, captor.getValue().getSectores().size());
        assertEquals(Integer.valueOf(120), captor.getValue().getSectores().get(0).getCapacidadTotal(),
                "ventas abre el cupo del evento, no la capacidad del sector");
    }

    @Test
    void publicar_conElLugarPorDefinir_throws() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(POR_DEFINIR_ID, "Por definir", "Por definir"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        BusinessConflictException e = assertThrows(BusinessConflictException.class,
                () -> service.publicar(EVENTO_ID, ORG_ID, "ORGANIZADOR"));
        assertTrue(e.getMessage().contains("elegí un lugar"));
    }

    @Test
    void actualizar_borrador_cambiarDeLugar_descartaLosPreciosDelLugarAnterior() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Viejo", "Vieja"));
        Lugar nuevo = lugar("f002", "Nuevo", "Nueva");
        List<PrecioSector> viejos = List.of(precioSector(sector(SECTOR_ID, evento.getLugar(), "Campo", 500, false), BigDecimal.TEN));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(lugarRepository.findById("f002")).thenReturn(Optional.of(nuevo));
        when(precioSectorRepository.findByEventoId(EVENTO_ID)).thenReturn(viejos);
        EventoRequestDTO req = new EventoRequestDTO();
        req.setIdLugar("f002");

        service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR");

        assertEquals("f002", evento.getLugar().getId());
        verify(precioSectorRepository).deleteAll(viejos);
    }

    @Test
    void actualizar_conElMismoLugarOSinLugar_noCambiaNadaDelLugar() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(lugarRepository.findById(LUGAR_ID)).thenReturn(Optional.of(evento.getLugar()));

        EventoRequestDTO mismo = new EventoRequestDTO();
        mismo.setIdLugar(LUGAR_ID);
        service.actualizar(EVENTO_ID, mismo, ORG_ID, "ORGANIZADOR");
        service.actualizar(EVENTO_ID, new EventoRequestDTO(), ORG_ID, "ORGANIZADOR");

        assertEquals(LUGAR_ID, evento.getLugar().getId());
        verify(precioSectorRepository, times(0)).deleteAll(any());
    }

    @Test
    void actualizar_publicado_noPuedeCambiarDeLugar_409() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Viejo", "Vieja"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(lugarRepository.findById("f002")).thenReturn(Optional.of(lugar("f002", "Nuevo", "Nueva")));
        EventoRequestDTO req = new EventoRequestDTO();
        req.setIdLugar("f002");

        assertThrows(BusinessConflictException.class, () -> service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR"));
        assertEquals(LUGAR_ID, evento.getLugar().getId());
    }

    @Test
    void actualizar_borrador_losSectoresSonLaListaCompleta_yLosOmitidosSeQuitan() {
        Evento evento = evento("BORRADOR", ORG_ID);
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        evento.setLugar(lugar);
        Sector campo = sector(SECTOR_ID, lugar, "Campo", 500, false);
        Sector platea = sector("s-platea", lugar, "Platea", 200, true);
        PrecioSector psCampo = precioSector(campo, BigDecimal.valueOf(10));
        PrecioSector psPlatea = precioSector(platea, BigDecimal.valueOf(20));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(campo));
        when(precioSectorRepository.findByEventoId(EVENTO_ID)).thenReturn(List.of(psCampo, psPlatea));
        EventoRequestDTO req = new EventoRequestDTO();
        req.setSectores(List.of(elegir(SECTOR_ID, 40000)));

        service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR");

        verify(precioSectorRepository).delete(psPlatea);
        verify(precioSectorRepository, times(0)).delete(psCampo);
    }

    @Test
    void actualizar_sinSectoresEnElPedido_noTocaLosSectoresDelEvento() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(LUGAR_ID, "Estadio", "CABA"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        EventoRequestDTO req = new EventoRequestDTO();
        req.setNombre("Solo cambio el nombre");

        service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR");

        assertEquals("Solo cambio el nombre", evento.getNombre());
        verify(precioSectorRepository, times(0)).delete(any(PrecioSector.class));
        verify(precioSectorRepository, times(0)).findByEventoId(any());
    }

    @Test
    void actualizar_conElLugarPorDefinir_noSePuedenDefinirSectores() {
        Evento evento = evento("BORRADOR", ORG_ID);
        evento.setLugar(lugar(POR_DEFINIR_ID, "Por definir", "Por definir"));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        EventoRequestDTO req = new EventoRequestDTO();
        req.setSectores(List.of(elegir(SECTOR_ID, 1000)));

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(EVENTO_ID, req, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void actualizar_publicado_soloCambiaElPrecioDeSectoresQueYaEstaban() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        Lugar lugar = lugar(LUGAR_ID, "Estadio", "CABA");
        evento.setLugar(lugar);
        Sector campo = sector(SECTOR_ID, lugar, "Campo", 500, false);
        PrecioSector actual = precioSector(campo, BigDecimal.valueOf(10));
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(campo));
        when(precioSectorRepository.findByEventoId(EVENTO_ID)).thenReturn(List.of(actual));
        when(precioSectorRepository.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_ID)).thenReturn(Optional.of(actual));

        EventoRequestDTO soloPrecio = new EventoRequestDTO();
        soloPrecio.setSectores(List.of(elegir(SECTOR_ID, 99000)));
        service.actualizar(EVENTO_ID, soloPrecio, ORG_ID, "ORGANIZADOR");
        assertEquals(0, BigDecimal.valueOf(99000).compareTo(actual.getPrecio()));

        // cambiar el cupo de un evento publicado: el stock ya está abierto en ventas
        SectorInputDTO otroCupo = elegir(SECTOR_ID, 99000);
        otroCupo.setCapacidadTotal(100);
        EventoRequestDTO conCupo = new EventoRequestDTO();
        conCupo.setSectores(List.of(otroCupo));
        assertThrows(BusinessConflictException.class, () -> service.actualizar(EVENTO_ID, conCupo, ORG_ID, "ORGANIZADOR"));

        // agregar un sector que no estaba
        Sector platea = sector("s-platea", lugar, "Platea", 200, true);
        when(sectorRepository.findById("s-platea")).thenReturn(Optional.of(platea));
        EventoRequestDTO nuevoSector = new EventoRequestDTO();
        nuevoSector.setSectores(List.of(elegir("s-platea", 50000)));
        assertThrows(BusinessConflictException.class, () -> service.actualizar(EVENTO_ID, nuevoSector, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void listarLugares_ocultaElLugarPorDefinir_yNoSePuedeConsultarNiEditar() {
        when(lugarRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                lugar(POR_DEFINIR_ID, "Por definir", "Por definir"), lugar(LUGAR_ID, "Estadio", "CABA")));

        var lugares = service.listarLugares(null);

        assertEquals(1, lugares.size());
        assertEquals(LUGAR_ID, lugares.get(0).getId());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerLugar(POR_DEFINIR_ID));
        assertThrows(ResourceNotFoundException.class, () -> service.eliminarLugar(POR_DEFINIR_ID));
    }

    // ---------- listarAsientos ----------

    @Test
    void listarAsientos_sectorNoPerteneceAlLugar_throws() {
        Sector s = sector(SECTOR_ID, lugar("f002", "Otro", "Otra"), "X", 100, false);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));

        assertThrows(ResourceNotFoundException.class,
                () -> service.listarAsientos(LUGAR_ID, SECTOR_ID));
    }

    @Test
    void listarAsientos_sectorNumerado_generaAsientosVirtuales() {
        Sector s = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Platea", 6, true);
        s.setFilas(2);
        s.setAsientosPorFila(3);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));

        var res = service.listarAsientos(LUGAR_ID, SECTOR_ID);

        assertEquals(6, res.size());
        assertEquals("A-1", res.get(0).getCodigo());
        assertEquals("B-3", res.get(5).getCodigo());
        assertEquals(SECTOR_ID, res.get(0).getIdSector());
        assertNull(res.get(0).getId());
    }

    @Test
    void listarAsientos_sectorGeneral_vacio() {
        Sector s = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Campo", 500, false);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));

        assertEquals(0, service.listarAsientos(LUGAR_ID, SECTOR_ID).size());
    }

    @Test
    void listarAsientos_numeradoSinLayout_vacio() {
        Sector s = sector(SECTOR_ID, lugar(LUGAR_ID, "Estadio", "CABA"), "Platea", 300, true);
        when(sectorRepository.findById(SECTOR_ID)).thenReturn(Optional.of(s));

        assertEquals(0, service.listarAsientos(LUGAR_ID, SECTOR_ID).size());
    }

    // ---------- participantes ----------

    @Test
    void listarParticipantes_ok() {
        Participante p = new Participante();
        p.setId("d-1");
        p.setNombre("Artista");
        when(participanteRepository.buscar(null, null)).thenReturn(List.of(p));
        when(eventoMapper.toParticipante(p)).thenReturn(new ParticipanteResponseDTO());

        var res = service.listarParticipantes(null, null);

        assertEquals(1, res.size());
    }

    @Test
    void obtenerParticipante_ok() {
        Participante p = new Participante();
        p.setId("d-1");
        p.setNombre("Artista");
        when(participanteRepository.findById("d-1")).thenReturn(Optional.of(p));
        when(eventoMapper.toParticipante(p)).thenReturn(new ParticipanteResponseDTO());

        var res = service.obtenerParticipante("d-1");

        assertNotNull(res);
    }

    @Test
    void obtenerParticipante_noEncontrado_throws() {
        when(participanteRepository.findById("d-1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerParticipante("d-1"));
    }

    @Test
    void crearParticipante_bodyNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.crearParticipante(null));
    }

    @Test
    void crearParticipante_nombreNull_throws() {
        ParticipanteRequestDTO body = new ParticipanteRequestDTO();
        body.setNombre(null);
        assertThrows(IllegalArgumentException.class, () -> service.crearParticipante(body));
    }

    @Test
    void crearParticipante_ok() {
        ParticipanteRequestDTO body = new ParticipanteRequestDTO();
        body.setNombre("Artista");
        body.setTipo("ARTISTA");
        body.setDescripcion("Desc");
        Participante p = new Participante();
        p.setId("d-1");
        p.setNombre("Artista");
        p.setTipo("ARTISTA");
        p.setDescripcion("Desc");
        when(participanteRepository.save(any(Participante.class))).thenReturn(p);
        when(eventoMapper.toParticipante(p)).thenReturn(new ParticipanteResponseDTO());

        service.crearParticipante(body);

        verify(participanteRepository).save(any(Participante.class));
    }

    @Test
    void actualizarParticipante_bodyNull_throws() {
        when(participanteRepository.findById("d-1")).thenReturn(Optional.of(new Participante()));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarParticipante("d-1", null));
    }

    @Test
    void actualizarParticipante_ok() {
        Participante p = new Participante();
        p.setId("d-1");
        p.setNombre("Viejo");
        when(participanteRepository.findById("d-1")).thenReturn(Optional.of(p));
        when(participanteRepository.save(any(Participante.class))).thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toParticipante(p)).thenReturn(new ParticipanteResponseDTO());

        ParticipanteRequestDTO body = new ParticipanteRequestDTO();
        body.setNombre("Nuevo");

        service.actualizarParticipante("d-1", body);

        assertEquals("Nuevo", p.getNombre());
    }

    @Test
    void listarParticipantesDeEvento_ok() {
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento()));
        when(eventoParticipanteRepository.findByEventoIdOrdered(EVENTO_ID)).thenReturn(List.of());
        when(eventoMapper.toParticipantesEvento(List.of())).thenReturn(List.of());

        var res = service.listarParticipantesDeEvento(EVENTO_ID);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    void asociarParticipantes_conParticipantes_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        Participante p = new Participante();
        p.setId("d-1");
        p.setNombre("Artista");
        when(participanteRepository.findById("d-1")).thenReturn(Optional.of(p));
        when(eventoParticipanteRepository.save(any(EventoParticipante.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toParticipantesEvento(anyList())).thenReturn(List.of());

        EventoParticipantesRequestDTO body = new EventoParticipantesRequestDTO();
        EventoParticipantesRequestDTO.Item it = new EventoParticipantesRequestDTO.Item();
        it.setId("d-1");
        it.setRol("PRINCIPAL");
        it.setOrden(0);
        body.setParticipantes(List.of(it));

        service.asociarParticipantes(EVENTO_ID, body, ORG_ID, "ORGANIZADOR");

        verify(eventoParticipanteRepository).deleteByEventoId(EVENTO_ID);
        verify(eventoParticipanteRepository, times(1)).save(any(EventoParticipante.class));
    }

    @Test
    void asociarParticipantes_conIds_ok() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));
        Participante p = new Participante();
        p.setId("d-1");
        p.setNombre("Artista");
        when(participanteRepository.findById("d-1")).thenReturn(Optional.of(p));
        when(eventoParticipanteRepository.save(any(EventoParticipante.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(eventoMapper.toParticipantesEvento(anyList())).thenReturn(List.of());

        EventoParticipantesRequestDTO body = new EventoParticipantesRequestDTO();
        body.setIds(List.of("d-1"));

        service.asociarParticipantes(EVENTO_ID, body, ORG_ID, "ADMIN");

        verify(eventoParticipanteRepository, times(1)).save(any(EventoParticipante.class));
    }

    @Test
    void asociarParticipantes_bodyNull_throws() {
        Evento evento = evento("PUBLICADO", ORG_ID);
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(IllegalArgumentException.class,
                () -> service.asociarParticipantes(EVENTO_ID, null, ORG_ID, "ORGANIZADOR"));
    }

    @Test
    void asociarParticipantes_noEsDueño_throwsForbidden() {
        Evento evento = evento("PUBLICADO", "otro-org");
        when(eventoRepository.findByIdWithLugar(EVENTO_ID)).thenReturn(Optional.of(evento));

        EventoParticipantesRequestDTO body = new EventoParticipantesRequestDTO();
        body.setIds(List.of("d-1"));

        assertThrows(ForbiddenException.class,
                () -> service.asociarParticipantes(EVENTO_ID, body, ORG_ID, "ORGANIZADOR"));
    }

    // ---------- helpers ----------

    private Evento evento(String estado, String idOrganizador) {
        Evento e = new Evento();
        e.setId(EVENTO_ID);
        e.setIdOrganizador(idOrganizador);
        e.setNombre("Evento");
        e.setEstado(estado);
        e.setFechaInicio(LocalDateTime.of(2026, 3, 14, 20, 0));
        return e;
    }

    private Evento evento() {
        return evento("PUBLICADO", ORG_ID);
    }

    private Lugar lugar(String id, String nombre, String ciudad) {
        Lugar l = new Lugar();
        l.setId(id);
        l.setNombre(nombre);
        l.setCiudad(ciudad);
        l.setCapacidadTotal(0);
        return l;
    }

    private Sector sector(String id, Lugar lugar, String nombre, Integer cap, boolean numerado) {
        Sector s = new Sector();
        s.setId(id);
        s.setLugar(lugar);
        s.setNombre(nombre);
        s.setCapacidad(cap);
        s.setTipo(nombre.toUpperCase());
        s.setEsNumerado(numerado);
        return s;
    }

    private PrecioSector precioSector(Sector sector, BigDecimal precio) {
        PrecioSector ps = new PrecioSector();
        ps.setId("ps-1");
        ps.setSector(sector);
        ps.setPrecio(precio);
        ps.setMoneda("ARS");
        ps.setActivo(true);
        return ps;
    }

    private EventoResponseDTO dto(Evento e) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        return dto;
    }

    private eventos.dto.EventoDetalleResponseDTO detalle(Evento e) {
        eventos.dto.EventoDetalleResponseDTO dto = new eventos.dto.EventoDetalleResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        return dto;
    }
}
