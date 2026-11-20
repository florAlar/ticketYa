package ventas.service;

import org.springframework.stereotype.Service;
import ventas.dto.FilaEstadoResponseDTO;
import ventas.dto.FilaIngresoResponseDTO;
import ventas.dto.FilaPosicionResponseDTO;
import ventas.exception.FilaException;
import ventas.observability.Bitacora;
import ventas.redis.FilaRedisStore;

@Service
public class FilaServiceImpl implements FilaService {

    private static final String PUBLICADO = "PUBLICADO";

    private final FilaRedisStore store;
    private final FilaEventoCache eventos;

    public FilaServiceImpl(FilaRedisStore store, FilaEventoCache eventos) {
        this.store = store;
        this.eventos = eventos;
    }

    @Override
    public IngresoResultado ingresar(String idEvento, String userId) {
        FilaEventoCache.Info info = eventos.obtener(idEvento);
        validarElegible(info);

        FilaRedisStore.Ingreso ingreso = store.ingresar(idEvento, userId);
        long tanda = ingreso.tanda();
        Bitacora.registrar("FILA_INGRESO", userId, idEvento,
                "posicion=" + ingreso.posicion() + " creado=" + ingreso.creado());
        return new IngresoResultado(
                new FilaIngresoResponseDTO(idEvento, ingreso.posicion(), tanda,
                        delante(ingreso.posicion(), tanda), estadoTurno(ingreso.posicion(), tanda, false, false)),
                ingreso.creado());
    }

    @Override
    public FilaPosicionResponseDTO posicion(String idEvento, String userId) {
        FilaEventoCache.Info info = eventos.obtener(idEvento);
        validarElegible(info);

        FilaRedisStore.Posicion p = store.posicion(idEvento, userId);
        if (p.posicion() == null) {
            throw FilaException.noIngresado();
        }
        return new FilaPosicionResponseDTO(idEvento, p.posicion(), p.tanda(),
                delante(p.posicion(), p.tanda()),
                estadoTurno(p.posicion(), p.tanda(), p.conToken(), p.usado()));
    }

    @Override
    public FilaEstadoResponseDTO estado(String idEvento) {
        FilaEventoCache.Info info = eventos.obtener(idEvento);
        FilaRedisStore.Estado e = store.estado(idEvento);
        return new FilaEstadoResponseDTO(idEvento, e.totalEnFila(), e.tanda(),
                PUBLICADO.equals(info.estado()) && info.altaDemanda());
    }

    static void validarElegible(FilaEventoCache.Info info) {
        if (!PUBLICADO.equals(info.estado()) || !info.altaDemanda()) {
            throw FilaException.eventoNoElegible();
        }
    }

    static long delante(long posicion, long tanda) {
        return Math.max(0, posicion - tanda);
    }

    private static String estadoTurno(long posicion, long tanda, boolean conToken, boolean usado) {
        if (usado) {
            return "TURNO_USADO";
        }
        if (conToken) {
            return "CON_TOKEN";
        }
        return posicion <= tanda ? "HABILITADO" : "EN_ESPERA";
    }
}
