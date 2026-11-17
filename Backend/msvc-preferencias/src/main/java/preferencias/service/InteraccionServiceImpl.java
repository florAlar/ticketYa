package preferencias.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import preferencias.clients.EventosClient;
import preferencias.clients.EventosClient.EventoDetalleDTO;
import preferencias.clients.EventosClient.ParticipanteDTO;
import preferencias.document.InteraccionEvento;
import preferencias.dto.HistorialItemDTO;
import preferencias.dto.PageResponseDTO;
import preferencias.messaging.CompraConfirmadaDTO;
import preferencias.observability.Bitacora;
import preferencias.repository.InteraccionEventoRepository;

@Service
public class InteraccionServiceImpl implements InteraccionService {

    public static final String TIPO_COMPRA = "COMPRA";

    private static final Logger log = LoggerFactory.getLogger(InteraccionServiceImpl.class);

    private final InteraccionEventoRepository repository;
    private final EventosClient eventosClient;

    public InteraccionServiceImpl(
            InteraccionEventoRepository repository, EventosClient eventosClient) {
        this.repository = repository;
        this.eventosClient = eventosClient;
    }

    @Override
    public void registrarCompra(CompraConfirmadaDTO compra) {
        if (compra == null) {
            throw new IllegalArgumentException("Mensaje de compra vacío");
        }
        if (compra.compraId() == null || compra.compraId().isBlank()) {
            throw new IllegalArgumentException("compraId es obligatorio");
        }
        if (compra.clienteId() == null || compra.clienteId().isBlank()) {
            throw new IllegalArgumentException("clienteId es obligatorio");
        }
        if (compra.eventoId() == null || compra.eventoId().isBlank()) {
            throw new IllegalArgumentException("eventoId es obligatorio");
        }
        registrarCompra(
                compra.compraId().trim(),
                compra.clienteId().trim(),
                compra.eventoId().trim(),
                compra.total(),
                compra.cantidadEntradas(),
                compra.occurredAt());
    }

    @Override
    public InteraccionEvento registrarCompra(
            String idCompra,
            String idCliente,
            String idEvento,
            BigDecimal monto,
            Integer cantidadEntradas,
            Instant fechaCompra) {
        InteraccionEvento doc = repository.findByIdCompra(idCompra).orElseGet(InteraccionEvento::new);
        doc.setIdCompra(idCompra);
        doc.setIdCliente(idCliente);
        doc.setIdEvento(idEvento);
        doc.setTipo(TIPO_COMPRA);
        if (monto != null) {
            doc.setMonto(monto);
        }
        if (cantidadEntradas != null) {
            doc.setCantidadEntradas(cantidadEntradas);
        }
        if (fechaCompra != null) {
            doc.setFechaCompra(fechaCompra);
        }
        if (doc.getFechaCompra() == null) {
            doc.setFechaCompra(Instant.now());
        }
        enriquecerEvento(doc);
        InteraccionEvento guardado = repository.save(doc);
        Bitacora.registrar(
                "INTERACCION_COMPRA_REGISTRADA",
                idCliente,
                idCompra,
                "evento=" + idEvento + " monto=" + monto + " cantidad=" + cantidadEntradas);
        return guardado;
    }

    /** Snapshot del evento best-effort: si eventos no responde, la interacción se guarda igual. */
    private void enriquecerEvento(InteraccionEvento doc) {
        if (doc.getIdEvento() == null || doc.getIdEvento().isBlank()) {
            return;
        }
        if (doc.getNombreEvento() != null && doc.getArtistas() != null && !doc.getArtistas().isEmpty()) {
            return;
        }
        try {
            EventoDetalleDTO evento = eventosClient.getEvento(doc.getIdEvento());
            if (evento == null) {
                return;
            }
            if (evento.getNombre() != null) {
                doc.setNombreEvento(evento.getNombre());
            }
            if (evento.getTipo() != null) {
                doc.setCategoria(evento.getTipo().trim().toUpperCase());
            }
            if (evento.getLugar() != null && evento.getLugar().getCiudad() != null) {
                doc.setCiudad(evento.getLugar().getCiudad());
            }
            if (evento.getFechaInicio() != null) {
                doc.setFechaEvento(toInstant(evento.getFechaInicio()));
            }
            List<String> artistas = evento.getParticipantes() == null
                    ? List.of()
                    : evento.getParticipantes().stream()
                            .map(ParticipanteDTO::getNombre)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .toList();
            doc.setArtistas(new ArrayList<>(artistas));
        } catch (RuntimeException ex) {
            log.warn("No se pudo enriquecer la interacción con datos del evento {}: {}",
                    doc.getIdEvento(), ex.getMessage());
        }
    }

    private static Instant toInstant(LocalDateTime fecha) {
        return fecha.atZone(ZoneOffset.UTC).toInstant();
    }

    @Override
    public PageResponseDTO<HistorialItemDTO> historial(String idCliente, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(
                safePage, safeSize, Sort.by(Sort.Direction.DESC, "fechaCompra"));
        Page<InteraccionEvento> result =
                repository.findByIdClienteAndTipo(idCliente, TIPO_COMPRA, pageable);
        List<HistorialItemDTO> content = result.getContent().stream()
                .map(this::toItem)
                .toList();
        return new PageResponseDTO<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private HistorialItemDTO toItem(InteraccionEvento doc) {
        HistorialItemDTO dto = new HistorialItemDTO();
        dto.setId(doc.getId());
        dto.setTipo(doc.getTipo());
        dto.setIdEvento(doc.getIdEvento());
        dto.setNombreEvento(doc.getNombreEvento());
        dto.setCategoria(doc.getCategoria());
        dto.setCiudad(doc.getCiudad());
        dto.setArtistas(doc.getArtistas() == null ? List.of() : doc.getArtistas());
        dto.setFechaEvento(doc.getFechaEvento());
        dto.setFechaCompra(doc.getFechaCompra());
        dto.setMonto(doc.getMonto());
        dto.setCantidadEntradas(doc.getCantidadEntradas());
        return dto;
    }

    @Override
    public boolean existeCompra(String idCompra) {
        return idCompra != null && !idCompra.isBlank()
                && repository.findByIdCompra(idCompra.trim()).isPresent();
    }

    @Override
    public List<InteraccionEvento> comprasDeCliente(String idCliente) {
        if (idCliente == null || idCliente.isBlank()) {
            return List.of();
        }
        return repository.findByIdClienteAndTipoOrderByFechaCompraDesc(idCliente.trim(), TIPO_COMPRA);
    }

    @Override
    public List<InteraccionEvento> comprasDeEvento(String idEvento) {
        if (idEvento == null || idEvento.isBlank()) {
            return List.of();
        }
        return repository.findByIdEventoAndTipo(idEvento.trim(), TIPO_COMPRA);
    }

    @Override
    public List<InteraccionEvento> comprasDeEventos(Collection<String> idEventos) {
        if (idEventos == null || idEventos.isEmpty()) {
            return List.of();
        }
        List<String> ids = idEventos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findByIdEventoInAndTipo(ids, TIPO_COMPRA);
    }

    @Override
    public List<InteraccionEvento> comprasDeClientes(Collection<String> idClientes) {
        if (idClientes == null || idClientes.isEmpty()) {
            return List.of();
        }
        List<String> ids = idClientes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findByIdClienteInAndTipo(ids, TIPO_COMPRA);
    }
}
