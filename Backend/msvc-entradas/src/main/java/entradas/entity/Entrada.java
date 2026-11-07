package entradas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "entrada")
@Getter
@Setter
public class Entrada {

    @Id
    @Column(name = "id_entrada", length = 36, nullable = false)
    private String idEntrada;

    /** Ref lógica ventas.compra */
    @Column(name = "id_compra", length = 36, nullable = false)
    private String idCompra;

    /** Ref lógica usuarios.usuario */
    @Column(name = "id_cliente", length = 36, nullable = false)
    private String idCliente;

    /** Ref lógica eventos.evento */
    @Column(name = "id_evento", length = 36, nullable = false)
    private String idEvento;

    /** Ref lógica eventos.sector */
    @Column(name = "id_sector", length = 36, nullable = false)
    private String idSector;

    /** Precio unitario al emitir (DTO + monto devolución/reintegro). */
    @Column(name = "precio", precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "codigo_qr", length = 128, nullable = false, unique = true)
    private String codigoQr;

    @Column(name = "estado", length = 32, nullable = false)
    private String estado;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;
}
