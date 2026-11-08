package ventas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reintenta emisión de QR para compras aprobadas aún PENDIENTE. */
@Component
public class EmisionPendienteJob {

    private static final Logger log = LoggerFactory.getLogger(EmisionPendienteJob.class);

    private final EmisionEntradasService emisionEntradasService;

    public EmisionPendienteJob(EmisionEntradasService emisionEntradasService) {
        this.emisionEntradasService = emisionEntradasService;
    }

    @Scheduled(fixedDelayString = "${ticketya.ventas.emision-job-ms:30000}")
    public void tick() {
        int n = emisionEntradasService.reprocesarPendientes();
        if (n > 0) {
            log.info("Emisiones recuperadas en job: {}", n);
        }
    }
}
