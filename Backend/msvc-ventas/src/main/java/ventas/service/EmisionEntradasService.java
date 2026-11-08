package ventas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ventas.clients.EntradasClient;
import ventas.entity.Compra;
import ventas.entity.DetalleReserva;
import ventas.entity.Reserva;
import ventas.repository.CompraRepository;
import ventas.repository.DetalleReservaRepository;

/**
 * Emisión de entradas (QR) tras pago aprobado.
 * Reintentos inmediatos + cola simple por estado PENDIENTE (sin tabla outbox).
 */
@Service
public class EmisionEntradasService {

    private static final Logger log = LoggerFactory.getLogger(EmisionEntradasService.class);

    public static final String EMISION_NO_REQUIERE = "NO_REQUIERE";
    public static final String EMISION_PENDIENTE = "PENDIENTE";
    public static final String EMISION_EMITIDA = "EMITIDA";
    public static final String EMISION_FALLIDA = "FALLIDA";

    private final CompraRepository compraRepository;
    private final DetalleReservaRepository detalleReservaRepository;
    private final EntradasClient entradasClient;
    /** Self-proxy para @Transactional(REQUIRES_NEW) en reintentos. */
    private final EmisionEntradasService self;

    private final int intentosInmediatos;
    private final long delayMs;
    private final int maxIntentos;

    public EmisionEntradasService(
            CompraRepository compraRepository,
            DetalleReservaRepository detalleReservaRepository,
            EntradasClient entradasClient,
            @Lazy EmisionEntradasService self,
            @Value("${ticketya.ventas.emision-intentos-inmediatos:3}") int intentosInmediatos,
            @Value("${ticketya.ventas.emision-delay-ms:250}") long delayMs,
            @Value("${ticketya.ventas.emision-max-intentos:20}") int maxIntentos) {
        this.compraRepository = compraRepository;
        this.detalleReservaRepository = detalleReservaRepository;
        this.entradasClient = entradasClient;
        this.self = self;
        this.intentosInmediatos = Math.max(1, intentosInmediatos);
        this.delayMs = Math.max(0, delayMs);
        this.maxIntentos = Math.max(intentosInmediatos, maxIntentos);
    }

    /** Marca la compra para emitir y agenda el intento cuando el pago ya quedó commit. */
    public void marcarPendienteYAgendar(Compra compra) {
        compra.setEmisionEstado(EMISION_PENDIENTE);
        if (compra.getEmisionIntentos() == null) {
            compra.setEmisionIntentos(0);
        }
        compra.setEmisionUltimoError(null);
        compra.setEmisionActualizado(nowUtc());
        String idCompra = compra.getIdCompra();
        scheduleAfterCommit(() -> intentarConReintentosInmediatos(idCompra));
    }

    public void intentarConReintentosInmediatos(String idCompra) {
        for (int i = 0; i < intentosInmediatos; i++) {
            if (self.intentarUnaVez(idCompra)) {
                return;
            }
            if (i + 1 < intentosInmediatos && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("Emisión aún pendiente tras {} intentos inmediatos (compra {})", intentosInmediatos, idCompra);
    }

    /**
     * @return true si quedó EMITIDA (o ya lo estaba)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean intentarUnaVez(String idCompra) {
        Compra compra = compraRepository.findById(idCompra).orElse(null);
        if (compra == null) {
            return false;
        }
        if (!VentaServiceImpl.EST_COMPRA_APROBADA.equals(compra.getEstado())) {
            return false;
        }
        if (EMISION_EMITIDA.equals(compra.getEmisionEstado())) {
            return true;
        }
        int intentos = compra.getEmisionIntentos() != null ? compra.getEmisionIntentos() : 0;
        if (intentos >= maxIntentos) {
            if (!EMISION_FALLIDA.equals(compra.getEmisionEstado())) {
                compra.setEmisionEstado(EMISION_FALLIDA);
                compra.setEmisionUltimoError(trimErr("máx intentos (" + maxIntentos + ") sin emitir"));
                compra.setEmisionActualizado(nowUtc());
                compraRepository.save(compra);
                log.error("Emisión FALLIDA definitiva compra {} tras {} intentos", idCompra, intentos);
            }
            return false;
        }

        Reserva reserva = compra.getReserva();
        if (reserva == null) {
            failAttempt(compra, "compra sin reserva");
            return false;
        }
        List<DetalleReserva> detalles =
                detalleReservaRepository.findByReservaIdReserva(reserva.getIdReserva());
        if (detalles == null || detalles.isEmpty()) {
            failAttempt(compra, "reserva sin detalle");
            return false;
        }

        try {
            List<EntradasClient.EmitirRequest.Item> items = detalles.stream()
                    .map(d -> new EntradasClient.EmitirRequest.Item(
                            d.getIdSector(),
                            d.getCantidad(),
                            d.getPrecioUnitario() != null ? d.getPrecioUnitario() : BigDecimal.ZERO))
                    .toList();
            entradasClient.emitir(new EntradasClient.EmitirRequest(
                    compra.getIdCompra(),
                    compra.getIdCliente(),
                    reserva.getIdEvento(),
                    items));
            compra.setEmisionEstado(EMISION_EMITIDA);
            compra.setEmisionIntentos(intentos + 1);
            compra.setEmisionUltimoError(null);
            compra.setEmisionActualizado(nowUtc());
            compraRepository.save(compra);
            log.info("Entradas emitidas para compra {} (intento {})", idCompra, intentos + 1);
            return true;
        } catch (Exception ex) {
            failAttempt(compra, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            log.error("No se pudieron emitir entradas para compra {}: {}", idCompra, ex.getMessage());
            return false;
        }
    }

    /** Job: reprocesa compras APROBADA + PENDIENTE. */
    public int reprocesarPendientes() {
        List<Compra> pendientes = compraRepository.findByEstadoAndEmisionEstadoOrderByFechaCompraAsc(
                VentaServiceImpl.EST_COMPRA_APROBADA, EMISION_PENDIENTE);
        int ok = 0;
        for (Compra c : pendientes) {
            if (self.intentarUnaVez(c.getIdCompra())) {
                ok++;
            }
        }
        return ok;
    }

    private void failAttempt(Compra compra, String err) {
        int intentos = (compra.getEmisionIntentos() != null ? compra.getEmisionIntentos() : 0) + 1;
        compra.setEmisionIntentos(intentos);
        compra.setEmisionUltimoError(trimErr(err));
        compra.setEmisionActualizado(nowUtc());
        if (intentos >= maxIntentos) {
            compra.setEmisionEstado(EMISION_FALLIDA);
        } else {
            compra.setEmisionEstado(EMISION_PENDIENTE);
        }
        compraRepository.save(compra);
    }

    private void scheduleAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private static String trimErr(String err) {
        if (err == null) {
            return null;
        }
        String t = err.trim();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
