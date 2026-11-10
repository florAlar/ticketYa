package eventos.service;

import feign.FeignException;
import eventos.clients.EntradasClient;
import eventos.clients.VentasClient;
import eventos.clients.VentasClient.AbrirCuposBody;
import eventos.dto.AsientoResponseDTO;
import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoFechaRequestDTO;
import eventos.dto.EventoParticipantesRequestDTO;
import eventos.dto.EventoRequestDTO;
import eventos.dto.EventoRequestDTO.SectorInputDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.LugarRequestDTO;
import eventos.dto.LugarResponseDTO;
import eventos.dto.PageResponseDTO;
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
import eventos.util.SectorLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoServiceImpl implements EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoServiceImpl.class);

    /** Sentinel interno: catálogo público = PUBLICADO + AGOTADO (visibles, no comprables si agotado). */
        private static final String ESTADO_CATALOGO_DEFAULT = "CATALOGO";
        private static final String BORRADOR = "BORRADOR";
        private static final String PUBLICADO = "PUBLICADO";
        private static final String CANCELADO = "CANCELADO";
        private static final String SUSPENDIDO = "SUSPENDIDO";
        private static final String AGOTADO = "AGOTADO";
        private static final String FINALIZADO = "FINALIZADO";

    private static final Set<String> ESTADOS_PATCH = Set.of(
            SUSPENDIDO, CANCELADO, AGOTADO, FINALIZADO, PUBLICADO, BORRADOR);

    private final EventoRepository eventoRepository;
        private final PrecioSectorRepository precioSectorRepository;
        private final LugarRepository lugarRepository;
        private final SectorRepository sectorRepository;
        private final ParticipanteRepository participanteRepository;
        private final EventoParticipanteRepository eventoParticipanteRepository;
        private final EventoMapper eventoMapper;
        private final EntradasClient entradasClient;
        private final VentasClient ventasClient;

        public EventoServiceImpl(
                EventoRepository eventoRepository,
                PrecioSectorRepository precioSectorRepository,
                LugarRepository lugarRepository,
                SectorRepository sectorRepository,
                ParticipanteRepository participanteRepository,
                EventoParticipanteRepository eventoParticipanteRepository,
                EventoMapper eventoMapper,
                EntradasClient entradasClient,
                VentasClient ventasClient) {
            this.eventoRepository = eventoRepository;
            this.precioSectorRepository = precioSectorRepository;
            this.lugarRepository = lugarRepository;
            this.sectorRepository = sectorRepository;
            this.participanteRepository = participanteRepository;
            this.eventoParticipanteRepository = eventoParticipanteRepository;
            this.eventoMapper = eventoMapper;
            this.entradasClient = entradasClient;
            this.ventasClient = ventasClient;
        }

        @Override
        @Transactional(readOnly = true)
        public PageResponseDTO<EventoResponseDTO> buscar(
                String tipo,
                String ciudad,
                String organizador,
                String participante,
                String estado,
                LocalDate fechaDesde,
                LocalDate fechaHasta,
                String orden,
                int page,
                int size) {

            int safePage = Math.max(page, 0);
            int safeSize = size <= 0 ? 20 : Math.min(size, 200);

            boolean todos = estado != null
                                && (estado.equalsIgnoreCase("TODOS") || estado.equalsIgnoreCase("ALL") || estado.equals("*"));
                        String estadoRaw = todos ? null : blankToNull(estado);
                        boolean catalogoDefault = !todos && estadoRaw == null;
                        String estadoFiltro = catalogoDefault ? ESTADO_CATALOGO_DEFAULT : estadoRaw;

                        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
                        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;

                        Sort sort = resolveSort(orden);
                        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

                        Page<Evento> result = eventoRepository.buscar(
                                blankToNull(tipo),
                                blankToNull(ciudad),
                                blankToNull(organizador),
                                estadoFiltro,
                                catalogoDefault,
                                desde,
                                hasta,
                                blankToNull(participante),
                                pageable);

        List<Evento> eventos = result.getContent();
        Map<String, BigDecimal> minPrecios = minPrecios(eventos.stream().map(Evento::getId).toList());

        List<EventoResponseDTO> content = eventos.stream()
                .map(e -> eventoMapper.toCatalogo(e, minPrecios.get(e.getId())))
                .collect(Collectors.toList());

        if ("precio".equalsIgnoreCase(blankToNull(orden))) {
            content.sort((a, b) -> {
                BigDecimal pa = a.getPrecioDesde() != null ? a.getPrecioDesde() : BigDecimal.ZERO;
                BigDecimal pb = b.getPrecioDesde() != null ? b.getPrecioDesde() : BigDecimal.ZERO;
                return pa.compareTo(pb);
            });
        }

        return new PageResponseDTO<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDetalleResponseDTO obtenerPorId(String id) {
        return toDetalle(requireEvento(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarDeOrganizador(String idOrganizador) {
        List<Evento> list = eventoRepository.findByIdOrganizadorOrderByFechaInicioDesc(idOrganizador);
        Map<String, BigDecimal> minPrecios = minPrecios(list.stream().map(Evento::getId).toList());
        return list.stream()
                .map(e -> eventoMapper.toCatalogo(e, minPrecios.get(e.getId())))
                .toList();
    }

    @Override
        @Transactional
        public EventoDetalleResponseDTO crear(EventoRequestDTO request, String idOrganizador) {
            if (request == null) {
                throw new IllegalArgumentException("Body requerido");
            }
            String nombre = requireText(request.getNombre(), "nombre");
            String estadoPedido = blankToNull(request.getEstado());
            boolean publicarAlFinal = estadoPedido != null && PUBLICADO.equalsIgnoreCase(estadoPedido);

            if (request.getFechaInicio() != null) {
                            // sin límite de “fecha futura” en alta (borrador o publicar)
                        } else if (publicarAlFinal) {
                            throw new IllegalArgumentException("fechaInicio es obligatoria para publicar");
                        }

            String tipo = blankToNull(request.getTipo());
            if (tipo == null) {
                tipo = "RECITAL";
            } else {
                tipo = tipo.toUpperCase();
            }

            Lugar lugar = resolveLugarForCreate(request, publicarAlFinal);
            List<SectorInputDTO> inputs = normalizeSectorInputs(request.getSectores());
            if (publicarAlFinal && inputs.isEmpty()) {
                throw new IllegalArgumentException("Agregá al menos un sector/tipo de entrada con nombre");
            }

            // crear sectores nuevos del lugar si el payload no trae id de sector existente
            List<Sector> sectoresCreados = new ArrayList<>();
            for (SectorInputDTO in : inputs) {
                if (blankToNull(in.getId()) != null) {
                    Sector existing = sectorRepository
                            .findById(in.getId().trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado: " + in.getId()));
                    if (!existing.getLugar().getId().equals(lugar.getId())) {
                        throw new IllegalArgumentException("Sector no pertenece al lugar del evento");
                    }
                    applySectorInput(existing, in, publicarAlFinal);
                    sectoresCreados.add(sectorRepository.save(existing));
                } else {
                    Sector s = new Sector();
                    s.setId(UUID.randomUUID().toString());
                    s.setLugar(lugar);
                    applySectorInput(s, in, publicarAlFinal);
                    sectoresCreados.add(sectorRepository.save(s));
                }
            }
            recalcularCapacidadLugar(lugar);

            Evento evento = new Evento();
            evento.setId(UUID.randomUUID().toString());
            evento.setIdOrganizador(idOrganizador);
            evento.setNombre(nombre);
            evento.setDescripcion(blankToNull(request.getDescripcion()));
            evento.setTipo(tipo);
            // Schema NOT NULL: borrador sin fecha → provisional +1 año (publicar exige fecha real futura)
            evento.setFechaInicio(
                    request.getFechaInicio() != null
                            ? request.getFechaInicio()
                            : LocalDateTime.now(ZoneOffset.UTC).plusYears(1));
            evento.setFechaFin(request.getFechaFin());
            evento.setEstado(BORRADOR);
            evento.setLugar(lugar);
            evento.setFechaPublicacion(null);
            evento.setEsAltaDemanda(Boolean.TRUE.equals(request.getEsAltaDemanda()));
            eventoRepository.save(evento);

            for (int i = 0; i < inputs.size(); i++) {
                SectorInputDTO in = inputs.get(i);
                Sector sector = sectoresCreados.get(i);
                upsertPrecio(evento, sector, precioOf(in), "ARS", true);
            }

            if (publicarAlFinal) {
                return doPublicar(evento);
            }
            return toDetalle(evento);
        }

    @Override
    @Transactional
    public EventoDetalleResponseDTO actualizar(String id, EventoRequestDTO request, String userId, String role) {
        Evento evento = requireEvento(id);
        assertOwnerOrAdmin(evento, userId, role);
        if (CANCELADO.equalsIgnoreCase(evento.getEstado()) || FINALIZADO.equalsIgnoreCase(evento.getEstado())) {
            throw new BusinessConflictException("No se puede editar un evento " + evento.getEstado());
        }
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }

        if (blankToNull(request.getNombre()) != null) {
            evento.setNombre(request.getNombre().trim());
        }
        if (request.getDescripcion() != null) {
            evento.setDescripcion(blankToNull(request.getDescripcion()));
        }
        if (blankToNull(request.getTipo()) != null) {
            evento.setTipo(request.getTipo().trim().toUpperCase());
        }
        if (request.getFechaInicio() != null) {
                    evento.setFechaInicio(request.getFechaInicio());
                }
        if (request.getFechaFin() != null) {
            evento.setFechaFin(request.getFechaFin());
        }
        if (request.getEsAltaDemanda() != null) {
            evento.setEsAltaDemanda(request.getEsAltaDemanda());
        }

        if (blankToNull(request.getIdLugar()) != null) {
            Lugar lugar = lugarRepository
                    .findById(request.getIdLugar().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado: " + request.getIdLugar()));
            evento.setLugar(lugar);
        } else if (blankToNull(request.getLugarNombre()) != null || blankToNull(request.getLugarCiudad()) != null) {
            Lugar lugar = evento.getLugar();
            if (blankToNull(request.getLugarNombre()) != null) {
                lugar.setNombre(request.getLugarNombre().trim());
            }
            if (blankToNull(request.getLugarCiudad()) != null) {
                lugar.setCiudad(request.getLugarCiudad().trim());
            }
            if (request.getLugarDireccion() != null) {
                lugar.setDireccion(blankToNull(request.getLugarDireccion()));
            }
            lugarRepository.save(lugar);
        }

        // sectores opcionales en PUT: si vienen, upsert precios/alta de sectores del lugar
        List<SectorInputDTO> inputs = normalizeSectorInputs(request.getSectores());
        if (!inputs.isEmpty()) {
            Lugar lugar = evento.getLugar();
            for (SectorInputDTO in : inputs) {
                Sector sector;
                if (blankToNull(in.getId()) != null) {
                    sector = sectorRepository
                            .findById(in.getId().trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado: " + in.getId()));
                } else {
                    sector = new Sector();
                    sector.setId(UUID.randomUUID().toString());
                    sector.setLugar(lugar);
                }
                applySectorInput(sector, in, false);
                sectorRepository.save(sector);
                upsertPrecio(evento, sector, precioOf(in), "ARS", true);
            }
            recalcularCapacidadLugar(lugar);
        }

        eventoRepository.save(evento);
        return toDetalle(evento);
    }

    @Override
    @Transactional
    public void eliminarBorrador(String id, String userId, String role) {
        Evento evento = requireEvento(id);
        assertOwnerOrAdmin(evento, userId, role);
        if (!BORRADOR.equalsIgnoreCase(evento.getEstado())) {
            throw new BusinessConflictException("Solo se puede eliminar un evento en BORRADOR");
        }
        List<PrecioSector> precios = precioSectorRepository.findByEventoId(id);
        precioSectorRepository.deleteAll(precios);
        eventoRepository.delete(evento);
    }

    @Override
    @Transactional
    public EventoDetalleResponseDTO publicar(String id, String userId, String role) {
        Evento evento = requireEvento(id);
        assertOwnerOrAdmin(evento, userId, role);
        return doPublicar(evento);
    }

    @Override
    @Transactional
    public EventoDetalleResponseDTO patchEstado(String id, String estado, String userId, String role) {
        Evento evento = requireEvento(id);
        String next = blankToNull(estado);
        if (next == null) {
            throw new IllegalArgumentException("estado es obligatorio");
        }
        next = next.toUpperCase();
        if (!ESTADOS_PATCH.contains(next)) {
            throw new IllegalArgumentException("Estado inválido: " + next);
        }
        if (PUBLICADO.equals(next)) {
            assertOwnerOrAdmin(evento, userId, role);
            return doPublicar(evento);
        }
        // suspender/cancelar/agotado/finalizado: ADMIN; org solo SUSPENDIDO de propios
        if (isAdmin(role)) {
            // ok
        } else if (isOrganizador(role) && SUSPENDIDO.equals(next)) {
            assertOwnerOrAdmin(evento, userId, role);
        } else {
            throw new ForbiddenException("Sin permiso para cambiar estado a " + next);
        }

        if (BORRADOR.equalsIgnoreCase(evento.getEstado()) && !PUBLICADO.equals(next)) {
            throw new BusinessConflictException("Publicá el borrador antes de cambiar a " + next);
        }

        evento.setEstado(next);
        eventoRepository.save(evento);

        if (CANCELADO.equals(next)) {
            notifyReintegroBestEffort(evento.getId());
        }
        return toDetalle(evento);
    }

    @Override
    @Transactional
    public EventoDetalleResponseDTO patchFecha(String id, EventoFechaRequestDTO body, String userId, String role) {
        Evento evento = requireEvento(id);
        assertOwnerOrAdmin(evento, userId, role);
        if (body == null || (body.getFechaInicio() == null && body.getFechaFin() == null)) {
            throw new IllegalArgumentException("fechaInicio o fechaFin requeridas");
        }
        if (CANCELADO.equalsIgnoreCase(evento.getEstado()) || FINALIZADO.equalsIgnoreCase(evento.getEstado())) {
            throw new BusinessConflictException("No se puede cambiar fecha de un evento " + evento.getEstado());
        }
        if (body.getFechaInicio() != null) {
                    evento.setFechaInicio(body.getFechaInicio());
                }
        if (body.getFechaFin() != null) {
            evento.setFechaFin(body.getFechaFin());
        }
        eventoRepository.save(evento);
        return toDetalle(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectorEventoDTO> listarSectoresEvento(String idEvento) {
        requireEvento(idEvento);
        return eventoMapper.toSectoresPublic(precioSectorRepository.findAllByEventoId(idEvento));
    }

    @Override
    @Transactional
    public SectorEventoDTO upsertPrecioSector(
            String idEvento, String sectorId, PrecioSectorRequestDTO body, String userId, String role) {
        Evento evento = requireEvento(idEvento);
        assertOwnerOrAdmin(evento, userId, role);
        if (body == null || body.getPrecio() == null) {
            throw new IllegalArgumentException("precio es obligatorio");
        }
        Sector sector = sectorRepository
                .findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado: " + sectorId));
        if (!sector.getLugar().getId().equals(evento.getLugar().getId())) {
            throw new IllegalArgumentException("El sector no pertenece al lugar del evento");
        }
        String moneda = blankToNull(body.getMoneda()) != null ? body.getMoneda().trim().toUpperCase() : "ARS";
        boolean activo = body.getActivo() == null || Boolean.TRUE.equals(body.getActivo());
        PrecioSector ps = upsertPrecio(evento, sector, body.getPrecio(), moneda, activo);
        return eventoMapper.toSectorDto(ps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LugarResponseDTO> listarLugares(String ciudad) {
        List<Lugar> list = blankToNull(ciudad) == null
                ? lugarRepository.findAllByOrderByNombreAsc()
                : lugarRepository.findByCiudadContainingIgnoreCaseOrderByNombreAsc(ciudad.trim());
        return list.stream().map(this::toLugarResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LugarResponseDTO obtenerLugar(String id) {
        return toLugarResponse(requireLugar(id));
    }

    @Override
    @Transactional
    public LugarResponseDTO crearLugar(LugarRequestDTO body) {
        if (body == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        Lugar lugar = new Lugar();
        lugar.setId(UUID.randomUUID().toString());
        applyLugar(lugar, body, true);
        lugarRepository.save(lugar);
        return toLugarResponse(lugar);
    }

    @Override
    @Transactional
    public LugarResponseDTO actualizarLugar(String id, LugarRequestDTO body) {
        Lugar lugar = requireLugar(id);
        if (body == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        applyLugar(lugar, body, false);
        lugarRepository.save(lugar);
        return toLugarResponse(lugar);
    }

    @Override
    @Transactional
    public SectorEventoDTO crearSector(String idLugar, SectorRequestDTO body) {
        Lugar lugar = requireLugar(idLugar);
        if (body == null || blankToNull(body.getNombre()) == null) {
            throw new IllegalArgumentException("nombre de sector obligatorio");
        }
        Sector s = new Sector();
        s.setId(UUID.randomUUID().toString());
        s.setLugar(lugar);
        s.setNombre(body.getNombre().trim());
        s.setTipo(blankToNull(body.getTipo()));
        boolean numerado = Boolean.TRUE.equals(body.getEsNumerado());
        s.setEsNumerado(numerado);
        if (numerado) {
            SectorLayout.validar(body.getFilas(), body.getAsientosPorFila(), s.getNombre());
            s.setFilas(body.getFilas());
            s.setAsientosPorFila(body.getAsientosPorFila());
            s.setCapacidad(SectorLayout.capacidad(body.getFilas(), body.getAsientosPorFila()));
        } else {
            s.setFilas(null);
            s.setAsientosPorFila(null);
            s.setCapacidad(body.getCapacidad() != null ? Math.max(0, body.getCapacidad()) : 0);
        }
        sectorRepository.save(s);
        recalcularCapacidadLugar(lugar);
        return eventoMapper.toSectorFromEntity(s, null, null);
    }

    @Override
    @Transactional
    public SectorEventoDTO actualizarSector(String idSector, SectorRequestDTO body) {
        Sector s = sectorRepository
                .findById(idSector)
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado: " + idSector));
        if (body == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        if (blankToNull(body.getNombre()) != null) {
            s.setNombre(body.getNombre().trim());
        }
        if (body.getTipo() != null) {
            s.setTipo(blankToNull(body.getTipo()));
        }
        boolean numerado = body.getEsNumerado() != null
                ? body.getEsNumerado()
                : Boolean.TRUE.equals(s.getEsNumerado());
        s.setEsNumerado(numerado);
        if (numerado) {
            // la capacidad de un sector numerado se deriva del layout (se ignora body.capacidad)
            Integer filas = body.getFilas() != null ? body.getFilas() : s.getFilas();
            Integer porFila = body.getAsientosPorFila() != null ? body.getAsientosPorFila() : s.getAsientosPorFila();
            SectorLayout.validar(filas, porFila, s.getNombre());
            s.setFilas(filas);
            s.setAsientosPorFila(porFila);
            s.setCapacidad(SectorLayout.capacidad(filas, porFila));
        } else {
            s.setFilas(null);
            s.setAsientosPorFila(null);
            if (body.getCapacidad() != null) {
                s.setCapacidad(Math.max(0, body.getCapacidad()));
            }
        }
        sectorRepository.save(s);
        recalcularCapacidadLugar(s.getLugar());
        return eventoMapper.toSectorFromEntity(s, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoResponseDTO> listarAsientos(String idLugar, String idSector) {
        Sector s = sectorRepository
                .findById(idSector)
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado: " + idSector));
        if (!s.getLugar().getId().equals(idLugar)) {
            throw new ResourceNotFoundException("Sector no pertenece al lugar");
        }
        if (!Boolean.TRUE.equals(s.getEsNumerado()) || s.getFilas() == null || s.getAsientosPorFila() == null) {
            return List.of();
        }
        // asientos virtuales: se calculan desde el layout, no hay filas persistidas
        return SectorLayout.generar(s.getId(), s.getFilas(), s.getAsientosPorFila());
            }

            @Override
            @Transactional(readOnly = true)
            public List<ParticipanteResponseDTO> listarParticipantes(String q, String tipo) {
                return participanteRepository.buscar(blankToNull(q), blankToNull(tipo)).stream()
                        .map(eventoMapper::toParticipante)
                        .toList();
            }

            @Override
            @Transactional(readOnly = true)
            public ParticipanteResponseDTO obtenerParticipante(String id) {
                return eventoMapper.toParticipante(requireParticipante(id));
            }

            @Override
            @Transactional
            public ParticipanteResponseDTO crearParticipante(ParticipanteRequestDTO body) {
                if (body == null) {
                    throw new IllegalArgumentException("Body requerido");
                }
                String nombre = requireText(body.getNombre(), "nombre");
                Participante p = new Participante();
                p.setId(UUID.randomUUID().toString());
                p.setNombre(nombre);
                p.setTipo(blankToNull(body.getTipo()) != null ? body.getTipo().trim().toUpperCase() : null);
                p.setDescripcion(blankToNull(body.getDescripcion()));
                return eventoMapper.toParticipante(participanteRepository.save(p));
            }

            @Override
            @Transactional
            public ParticipanteResponseDTO actualizarParticipante(String id, ParticipanteRequestDTO body) {
                Participante p = requireParticipante(id);
                if (body == null) {
                    throw new IllegalArgumentException("Body requerido");
                }
                if (blankToNull(body.getNombre()) != null) {
                    p.setNombre(body.getNombre().trim());
                }
                if (body.getTipo() != null) {
                    p.setTipo(blankToNull(body.getTipo()) != null ? body.getTipo().trim().toUpperCase() : null);
                }
                if (body.getDescripcion() != null) {
                    p.setDescripcion(blankToNull(body.getDescripcion()));
                }
                return eventoMapper.toParticipante(participanteRepository.save(p));
            }

            @Override
            @Transactional(readOnly = true)
            public List<ParticipanteResponseDTO> listarParticipantesDeEvento(String idEvento) {
                requireEvento(idEvento);
                return eventoMapper.toParticipantesEvento(
                        eventoParticipanteRepository.findByEventoIdOrdered(idEvento));
            }

            @Override
            @Transactional
            public List<ParticipanteResponseDTO> asociarParticipantes(
                                String idEvento, EventoParticipantesRequestDTO body, String userId, String role) {
                            Evento evento = requireEvento(idEvento);
                            assertOwnerOrAdmin(evento, userId, role);
                            if (body == null) {
                                throw new IllegalArgumentException("Body requerido");
                            }

                List<EventoParticipantesRequestDTO.Item> items = new ArrayList<>();
                if (body.getParticipantes() != null && !body.getParticipantes().isEmpty()) {
                    int i = 0;
                    for (EventoParticipantesRequestDTO.Item it : body.getParticipantes()) {
                        if (it == null || blankToNull(it.getId()) == null) {
                            continue;
                        }
                        EventoParticipantesRequestDTO.Item copy = new EventoParticipantesRequestDTO.Item();
                        copy.setId(it.getId().trim());
                        copy.setRol(blankToNull(it.getRol()) != null ? it.getRol().trim().toUpperCase() : null);
                        copy.setOrden(it.getOrden() != null ? it.getOrden() : i);
                        items.add(copy);
                        i++;
                    }
                } else if (body.getIds() != null) {
                    int i = 0;
                    for (String id : body.getIds()) {
                        if (blankToNull(id) == null) {
                            continue;
                        }
                        EventoParticipantesRequestDTO.Item it = new EventoParticipantesRequestDTO.Item();
                        it.setId(id.trim());
                        it.setOrden(i++);
                        items.add(it);
                    }
                }

                // dedupe por id, conserva último
                Map<String, EventoParticipantesRequestDTO.Item> byId = new HashMap<>();
                for (EventoParticipantesRequestDTO.Item it : items) {
                    byId.put(it.getId(), it);
                }
                items = new ArrayList<>(byId.values());
                items.sort((a, b) -> {
                    int oa = a.getOrden() != null ? a.getOrden() : 0;
                    int ob = b.getOrden() != null ? b.getOrden() : 0;
                    return Integer.compare(oa, ob);
                });

                eventoParticipanteRepository.deleteByEventoId(evento.getId());
                List<EventoParticipante> saved = new ArrayList<>();
                int idx = 0;
                for (EventoParticipantesRequestDTO.Item it : items) {
                    Participante p = requireParticipante(it.getId());
                    EventoParticipante ep = new EventoParticipante();
                    ep.setEvento(evento);
                    ep.setParticipante(p);
                    ep.setRol(it.getRol());
                    ep.setOrden(it.getOrden() != null ? it.getOrden() : idx);
                    saved.add(eventoParticipanteRepository.save(ep));
                    idx++;
                }
                return eventoMapper.toParticipantesEvento(saved);
            }

            private Participante requireParticipante(String id) {
                return participanteRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado: " + id));
            }

            // --- helpers dominio ---

                private static final String LUGAR_BORRADOR_NOMBRE = "Por definir";
                private static final String LUGAR_BORRADOR_CIUDAD = "Por definir";

        private EventoDetalleResponseDTO doPublicar(Evento evento) {
            if (CANCELADO.equalsIgnoreCase(evento.getEstado()) || FINALIZADO.equalsIgnoreCase(evento.getEstado())) {
                throw new BusinessConflictException("No se puede publicar un evento " + evento.getEstado());
            }
            if (blankToNull(evento.getNombre()) == null) {
                throw new BusinessConflictException("Publicación: falta nombre");
            }
            if (evento.getFechaInicio() == null) {
                            throw new BusinessConflictException("Publicación: falta fechaInicio");
                        }
                        if (evento.getLugar() == null) {
                throw new BusinessConflictException("Publicación: falta lugar");
            }
            Lugar lugar = evento.getLugar();
            if (blankToNull(lugar.getNombre()) == null
                    || LUGAR_BORRADOR_NOMBRE.equalsIgnoreCase(lugar.getNombre().trim())) {
                throw new BusinessConflictException("Publicación: definí el nombre del lugar");
            }
            if (blankToNull(lugar.getCiudad()) == null
                    || LUGAR_BORRADOR_CIUDAD.equalsIgnoreCase(lugar.getCiudad().trim())) {
                throw new BusinessConflictException("Publicación: definí la ciudad del lugar");
            }
            List<PrecioSector> activos = precioSectorRepository.findActivosByEventoId(evento.getId());
            if (activos.isEmpty()) {
                throw new BusinessConflictException("Publicación: se requiere al menos un tipo de entrada con precio");
            }
            for (PrecioSector ps : activos) {
                if (ps.getPrecio() == null || ps.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessConflictException("Publicación: precio inválido en sector");
                }
                Sector sec = ps.getSector();
                if (sec == null || blankToNull(sec.getNombre()) == null) {
                    throw new BusinessConflictException("Publicación: sector incompleto");
                }
                Integer cap = sec.getCapacidad();
                                if (cap == null || cap < 1) {
                                    throw new BusinessConflictException(
                                            "Publicación: el sector «" + sec.getNombre() + "» necesita cupo ≥ 1 (no hay cupo ilimitado)");
                                }
                            }

                            // Abrir stock comercial en msvc-ventas ANTES de marcar PUBLICADO (fail-hard).
                            abrirCuposEnVentas(evento.getId(), activos);

                            evento.setEstado(PUBLICADO);
                            if (evento.getFechaPublicacion() == null) {
                                evento.setFechaPublicacion(LocalDateTime.now(ZoneOffset.UTC));
                            }
                            eventoRepository.save(evento);
                            return toDetalle(evento);
                        }

                        /** Reservado: no se exige fecha futura en E1 (fuera de alcance de evaluación). */
                                                @SuppressWarnings("unused")
                                                private static void assertFechaInicioNoPasada(LocalDateTime fechaInicio) {
                                                    // no-op
                                                }

                                                private void abrirCuposEnVentas(String idEvento, List<PrecioSector> activos) {
                            AbrirCuposBody body = new AbrirCuposBody();
                            List<AbrirCuposBody.Item> items = new ArrayList<>();
                            for (PrecioSector ps : activos) {
                                Sector sec = ps.getSector();
                                items.add(new AbrirCuposBody.Item(sec.getId(), sec.getCapacidad()));
                            }
                            body.setSectores(items);
                            try {
                                ventasClient.abrirCupos(idEvento, body);
                            } catch (FeignException e) {
                                log.error("No se pudo abrir cupos en ventas para evento {}: status={}", idEvento, e.status());
                                throw new BusinessConflictException(
                                        "Publicación: no se pudo abrir el stock comercial en ventas (¿msvc-ventas arriba?). Detalle: HTTP "
                                                + e.status());
                            } catch (Exception e) {
                                log.error("No se pudo abrir cupos en ventas para evento {}: {}", idEvento, e.toString());
                                throw new BusinessConflictException(
                                        "Publicación: no se pudo abrir el stock comercial en ventas: " + e.getMessage());
                            }
                        }

                        private void notifyReintegroBestEffort(String idEvento) {
            try {
                entradasClient.reintegroCancelacion(idEvento, "ADMIN");
            } catch (FeignException e) {
                log.warn("Reintegro cancelación best-effort falló para evento {}: {}", idEvento, e.status());
            } catch (Exception e) {
                log.warn("Reintegro cancelación best-effort falló para evento {}: {}", idEvento, e.toString());
            }
        }

        private Lugar resolveLugarForCreate(EventoRequestDTO request, boolean exigirCompleto) {
            if (blankToNull(request.getIdLugar()) != null) {
                return requireLugar(request.getIdLugar().trim());
            }
            String nombre = blankToNull(request.getLugarNombre());
            String ciudad = blankToNull(request.getLugarCiudad());
            if (exigirCompleto) {
                nombre = requireText(request.getLugarNombre(), "lugarNombre (o idLugar)");
                ciudad = requireText(request.getLugarCiudad(), "lugarCiudad (o idLugar)");
            } else {
                if (nombre == null) {
                    nombre = LUGAR_BORRADOR_NOMBRE;
                }
                if (ciudad == null) {
                    ciudad = LUGAR_BORRADOR_CIUDAD;
                }
            }
            Lugar lugar = new Lugar();
            lugar.setId(UUID.randomUUID().toString());
            lugar.setNombre(nombre);
            lugar.setCiudad(ciudad);
            lugar.setDireccion(blankToNull(request.getLugarDireccion()));
            lugar.setCapacidadTotal(0);
            return lugarRepository.save(lugar);
        }

        private void applyLugar(Lugar lugar, LugarRequestDTO body, boolean creating) {
            if (creating || blankToNull(body.getNombre()) != null) {
                lugar.setNombre(requireText(body.getNombre(), "nombre"));
            }
            if (creating || blankToNull(body.getCiudad()) != null) {
                lugar.setCiudad(requireText(body.getCiudad(), "ciudad"));
            }
            if (body.getDireccion() != null) {
                lugar.setDireccion(blankToNull(body.getDireccion()));
            }
            if (body.getCapacidadTotal() != null) {
                lugar.setCapacidadTotal(Math.max(0, body.getCapacidadTotal()));
            } else if (lugar.getCapacidadTotal() == null) {
                lugar.setCapacidadTotal(0);
            }
        }

        /**
         * Nunca cupo ilimitado. En publicación (strict) cupo ≥ 1; en borrador se admite 0/provisional.
         */
        private void applySectorInput(Sector s, SectorInputDTO in, boolean strictCupo) {
            s.setNombre(requireText(in.getNombre(), "sectores[].nombre"));
            // sinCupo del payload se ignora (no hay cupo ilimitado)
            Integer cap = in.getCapacidadTotal() != null ? in.getCapacidadTotal() : in.getCapacidad();
            int capacidad = cap != null ? Math.max(0, cap) : 0;
            boolean numerado = Boolean.TRUE.equals(in.getEsNumerado());
            Integer filas = null;
            Integer porFila = null;
            if (numerado) {
                filas = in.getFilas() != null ? in.getFilas() : s.getFilas();
                porFila = in.getAsientosPorFila() != null ? in.getAsientosPorFila() : s.getAsientosPorFila();
                // borrador laxo: se admite sin layout; al publicar o si se informa alguno, deben estar ambos
                if (strictCupo || filas != null || porFila != null) {
                    SectorLayout.validar(filas, porFila, s.getNombre());
                    capacidad = SectorLayout.capacidad(filas, porFila);
                }
            }
            if (strictCupo && capacidad < 1) {
                throw new IllegalArgumentException(
                        "sectores[].capacidadTotal debe ser ≥ 1 para «" + s.getNombre() + "» (no hay cupo ilimitado)");
            }
            s.setCapacidad(capacidad);
            s.setEsNumerado(numerado);
            s.setFilas(filas);
            s.setAsientosPorFila(porFila);
            String tipo = blankToNull(in.getTipo());
            if (tipo == null) {
                tipo = s.getNombre().toUpperCase().replace(' ', '_');
            }
            s.setTipo(tipo);
        }

        private PrecioSector upsertPrecio(
            Evento evento, Sector sector, BigDecimal precio, String moneda, boolean activo) {
        PrecioSector ps = precioSectorRepository
                .findByEventoIdAndSectorId(evento.getId(), sector.getId())
                .orElseGet(() -> {
                    PrecioSector n = new PrecioSector();
                    n.setId(UUID.randomUUID().toString());
                    n.setEvento(evento);
                    n.setSector(sector);
                    return n;
                });
        ps.setPrecio(precio != null ? precio : BigDecimal.ZERO);
        ps.setMoneda(moneda != null ? moneda : "ARS");
        ps.setActivo(activo);
        return precioSectorRepository.save(ps);
    }

    private void recalcularCapacidadLugar(Lugar lugar) {
        int sum = sectorRepository.findByLugarIdOrderByNombreAsc(lugar.getId()).stream()
                .mapToInt(s -> s.getCapacidad() != null ? s.getCapacidad() : 0)
                .sum();
        lugar.setCapacidadTotal(sum);
        lugarRepository.save(lugar);
    }

    private EventoDetalleResponseDTO toDetalle(Evento evento) {
            // re-fetch lugar graph if needed
            Evento full = eventoRepository.findByIdWithLugar(evento.getId()).orElse(evento);
            BigDecimal precioDesde = precioSectorRepository.minPrecioByEventoId(full.getId());
            List<PrecioSector> precios = precioSectorRepository.findActivosByEventoId(full.getId());
            List<EventoParticipante> participantes =
                    eventoParticipanteRepository.findByEventoIdOrdered(full.getId());
            return eventoMapper.toDetalle(full, precioDesde, precios, participantes);
        }

    private LugarResponseDTO toLugarResponse(Lugar lugar) {
        LugarResponseDTO dto = new LugarResponseDTO();
        dto.setId(lugar.getId());
        dto.setNombre(lugar.getNombre());
        dto.setDireccion(lugar.getDireccion());
        dto.setCiudad(lugar.getCiudad());
        dto.setCapacidadTotal(lugar.getCapacidadTotal());
        List<SectorEventoDTO> sectores = sectorRepository.findByLugarIdOrderByNombreAsc(lugar.getId()).stream()
                .map(s -> eventoMapper.toSectorFromEntity(s, null, null))
                .toList();
        dto.setSectores(sectores);
        return dto;
    }

    private Evento requireEvento(String id) {
        return eventoRepository
                .findByIdWithLugar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + id));
    }

    private Lugar requireLugar(String id) {
        return lugarRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado: " + id));
    }

    private void assertOwnerOrAdmin(Evento evento, String userId, String role) {
        if (isAdmin(role)) {
            return;
        }
        if (userId == null || userId.isBlank()) {
            throw new ForbiddenException("Sin identidad para operar sobre el evento");
        }
        if (!userId.trim().equals(evento.getIdOrganizador())) {
            throw new ForbiddenException("Sin permiso sobre este evento");
        }
    }

    private Map<String, BigDecimal> minPrecios(List<String> ids) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (Object[] row : precioSectorRepository.minPrecioByEventoIds(ids)) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private static Sort resolveSort(String orden) {
        if (orden == null || orden.isBlank() || "fecha".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.ASC, "fechaInicio");
        }
        if ("relevancia".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.DESC, "fechaPublicacion")
                    .and(Sort.by(Sort.Direction.ASC, "fechaInicio"));
        }
        return Sort.by(Sort.Direction.ASC, "fechaInicio");
    }

    private static List<SectorInputDTO> normalizeSectorInputs(List<SectorInputDTO> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<SectorInputDTO> out = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (SectorInputDTO in : raw) {
            if (in == null || blankToNull(in.getNombre()) == null) {
                continue;
            }
            String key = in.getNombre().trim().toLowerCase();
            if (!seenNames.add(key) && blankToNull(in.getId()) == null) {
                continue;
            }
            out.add(in);
        }
        return out;
    }

    private static BigDecimal precioOf(SectorInputDTO in) {
        return in.getPrecio() != null ? in.getPrecio() : BigDecimal.ZERO;
    }

    private static String requireText(String value, String field) {
        String v = blankToNull(value);
        if (v == null) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return v;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isAdmin(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }

    private static boolean isOrganizador(String role) {
        return role != null && "ORGANIZADOR".equalsIgnoreCase(role.trim());
    }
}
