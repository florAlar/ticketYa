package ventas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservaExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ReservaExpirationJob.class);

    private final VentaService ventaService;

    public ReservaExpirationJob(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    /** Cada 30s libera cupos de reservas vencidas. */
    @Scheduled(fixedDelayString = "${ticketya.ventas.expire-job-ms:30000}")
    public void tick() {
        int n = ventaService.expirarReservasVencidas();
        if (n > 0) {
            log.info("Expiradas {} reservas", n);
        }
    }
}
