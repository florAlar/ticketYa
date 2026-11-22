package entradas.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Claim de emisión por compra para que la emisión sea idempotente incluso con llamadas
 * concurrentes (post-pago + reintentos de Feign + job de respaldo).
 *
 * <p>{@code id_compra} es la PK de {@code emision_compra}: el primer request la inserta y crea las
 * entradas en la misma transacción; los demás reciben {@code false} y ventas reintenta. Si la
 * transacción dueña hace rollback, el claim desaparece y el reintento puede volver a intentar.
 */
@Repository
public class EmisionClaimRepository {

    private final JdbcTemplate jdbc;

    public EmisionClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return true si esta llamada tomó el claim; false si ya estaba tomado. */
    public boolean reclamar(String idCompra) {
        return jdbc.update(
                "INSERT IGNORE INTO emision_compra (id_compra) VALUES (?)", idCompra) == 1;
    }
}
