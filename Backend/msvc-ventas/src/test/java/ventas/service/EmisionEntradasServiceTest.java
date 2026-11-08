package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ventas.clients.EntradasClient;
import ventas.entity.Compra;
import ventas.entity.DetalleReserva;
import ventas.entity.Reserva;
import ventas.repository.CompraRepository;
import ventas.repository.DetalleReservaRepository;

@ExtendWith(MockitoExtension.class)
class EmisionEntradasServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private DetalleReservaRepository detalleReservaRepository;

    @Mock
    private EntradasClient entradasClient;

    private EmisionEntradasService service;

    private static final String ID_COMPRA = "x0000000-0000-4000-8000-000000000001";
    private static final String ID_RESERVA = "r0000000-0000-4000-8000-000000000001";
    private static final String ID_CLIENTE = "b0000000-0000-4000-8000-000000000001";
    private static final String ID_EVENTO = "e0000000-0000-4000-8000-000000000001";
    private static final String ID_SECTOR = "c0000000-0000-4000-8000-000000000011";

    @BeforeEach
    void setUp() throws Exception {
        reset(compraRepository, detalleReservaRepository, entradasClient);
        service = new EmisionEntradasService(
                compraRepository, detalleReservaRepository, entradasClient, null,
                3, 0, 20);
        // Inyectar self (auto-referencia @Lazy) mediante reflexión
        Field selfField = EmisionEntradasService.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(service, service);
    }

    private EmisionEntradasService crearServiceConSelf() {
        return service;
    }

    private Compra compraAprobada() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setEstado(VentaServiceImpl.EST_COMPRA_APROBADA);
        compra.setEmisionEstado(EmisionEntradasService.EMISION_NO_REQUIERE);
        compra.setEmisionIntentos(0);
        Reserva reserva = new Reserva();
        reserva.setIdReserva(ID_RESERVA);
        reserva.setIdEvento(ID_EVENTO);
        compra.setReserva(reserva);
        return compra;
    }

    private DetalleReserva detalle() {
        DetalleReserva d = new DetalleReserva();
        d.setIdDetalle("d1");
        d.setIdSector(ID_SECTOR);
        d.setCantidad(2);
        d.setPrecioUnitario(BigDecimal.valueOf(95000));
        return d;
    }

    @Test
    void marcarPendienteYAgendar_marcaPendiente() {
        Compra compra = compraAprobada();
        service.marcarPendienteYAgendar(compra);
        assertEquals(EmisionEntradasService.EMISION_PENDIENTE, compra.getEmisionEstado());
        assertEquals(Integer.valueOf(0), compra.getEmisionIntentos());
        assertNotNull(compra.getEmisionActualizado());
    }

    @Test
    void intentarUnaVez_compraNula_devuelveFalso() {
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.empty());
        assertFalse(service.intentarUnaVez(ID_COMPRA));
    }

    @Test
    void intentarUnaVez_compraNoAprobada_devuelveFalso() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setEstado(VentaServiceImpl.EST_COMPRA_RECHAZADA);
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        assertFalse(service.intentarUnaVez(ID_COMPRA));
    }

    @Test
    void intentarUnaVez_yaEmitida_devuelveVerdadero() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setEstado(VentaServiceImpl.EST_COMPRA_APROBADA);
        compra.setEmisionEstado(EmisionEntradasService.EMISION_EMITIDA);
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        assertTrue(service.intentarUnaVez(ID_COMPRA));
    }

    @Test
    void intentarUnaVez_maxIntentosAlcanzado_marcaFallida() {
        Compra compra = compraAprobada();
        compra.setEmisionIntentos(20); // == maxIntentos
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.intentarUnaVez(ID_COMPRA));
        assertEquals(EmisionEntradasService.EMISION_FALLIDA, compra.getEmisionEstado());
        verify(compraRepository).save(compra);
    }

    @Test
    void intentarUnaVez_compraSinReserva_fallaIntento() {
        Compra compra = new Compra();
        compra.setIdCompra(ID_COMPRA);
        compra.setEstado(VentaServiceImpl.EST_COMPRA_APROBADA);
        compra.setEmisionEstado(EmisionEntradasService.EMISION_NO_REQUIERE);
        compra.setEmisionIntentos(0);
        compra.setReserva(null);
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.intentarUnaVez(ID_COMPRA));
        assertEquals(EmisionEntradasService.EMISION_PENDIENTE, compra.getEmisionEstado());
        assertEquals(Integer.valueOf(1), compra.getEmisionIntentos());
        verify(compraRepository).save(compra);
    }

    @Test
    void intentarUnaVez_reservaSinDetalle_fallaIntento() {
        Compra compra = compraAprobada();
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA))
                .thenReturn(new ArrayList<>());
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.intentarUnaVez(ID_COMPRA));
        assertEquals(Integer.valueOf(1), compra.getEmisionIntentos());
    }

    @Test
    void intentarUnaVez_emitidoConExito_devuelveVerdadero() {
        Compra compra = compraAprobada();
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA))
                .thenReturn(List.of(detalle()));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(entradasClient.emitir(any())).thenReturn(new ArrayList<>());

        assertTrue(service.intentarUnaVez(ID_COMPRA));
        assertEquals(EmisionEntradasService.EMISION_EMITIDA, compra.getEmisionEstado());
        assertEquals(Integer.valueOf(1), compra.getEmisionIntentos());
        verify(entradasClient).emitir(any());
        verify(compraRepository).save(compra);
    }

    @Test
    void intentarUnaVez_emitidorLanzaExcepcion_fallaIntento() {
        Compra compra = compraAprobada();
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA))
                .thenReturn(List.of(detalle()));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(entradasClient.emitir(any())).thenThrow(new RuntimeException("falló"));

        assertFalse(service.intentarUnaVez(ID_COMPRA));
        assertEquals(EmisionEntradasService.EMISION_PENDIENTE, compra.getEmisionEstado());
        assertEquals(Integer.valueOf(1), compra.getEmisionIntentos());
        assertNotNull(compra.getEmisionUltimoError());
    }

    @Test
    void reprocesarPendientes_sinPendientes_devuelveCero() {
        when(compraRepository.findByEstadoAndEmisionEstadoOrderByFechaCompraAsc(
                VentaServiceImpl.EST_COMPRA_APROBADA, EmisionEntradasService.EMISION_PENDIENTE))
                .thenReturn(new ArrayList<>());
        assertEquals(0, service.reprocesarPendientes());
    }

    @Test
    void reprocesarPendientes_conPendientes_llamaIntentarUnaVez() {
        Compra compra = compraAprobada();
        compra.setEmisionEstado(EmisionEntradasService.EMISION_PENDIENTE);
        when(compraRepository.findByEstadoAndEmisionEstadoOrderByFechaCompraAsc(
                VentaServiceImpl.EST_COMPRA_APROBADA, EmisionEntradasService.EMISION_PENDIENTE))
                .thenReturn(List.of(compra));
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA))
                .thenReturn(List.of(detalle()));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(entradasClient.emitir(any())).thenReturn(new ArrayList<>());

        int ok = service.reprocesarPendientes();
        assertEquals(1, ok);
        assertEquals(EmisionEntradasService.EMISION_EMITIDA, compra.getEmisionEstado());
    }

    @Test
    void reprocesarPendientes_compraNoEmitida_noCuenta() {
        Compra compra = compraAprobada();
        compra.setEmisionEstado(EmisionEntradasService.EMISION_PENDIENTE);
        when(compraRepository.findByEstadoAndEmisionEstadoOrderByFechaCompraAsc(
                VentaServiceImpl.EST_COMPRA_APROBADA, EmisionEntradasService.EMISION_PENDIENTE))
                .thenReturn(List.of(compra));
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA))
                .thenReturn(List.of(detalle()));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(entradasClient.emitir(any())).thenThrow(new RuntimeException("falló"));

        int ok = service.reprocesarPendientes();
        assertEquals(0, ok);
    }

    @Test
    void scheduleAfterCommit_sinTransaccion_ejecutaDirectamente() throws Exception {
        Method method = EmisionEntradasService.class.getDeclaredMethod(
                "scheduleAfterCommit", Runnable.class);
        method.setAccessible(true);
        boolean[] ejecutado = {false};
        method.invoke(service, (Runnable) () -> ejecutado[0] = true);
        assertTrue(ejecutado[0]);
    }

    @Test
    void trimErr_recortaMensajeLargo() throws Exception {
        Method method = EmisionEntradasService.class.getDeclaredMethod(
                "trimErr", String.class);
        method.setAccessible(true);
        String largo = "a".repeat(300);
        String resultado = (String) method.invoke(service, largo);
        assertEquals(255, resultado.length());
    }

    @Test
    void trimErr_cortoYNulo() throws Exception {
        Method method = EmisionEntradasService.class.getDeclaredMethod(
                "trimErr", String.class);
        method.setAccessible(true);
        assertEquals("corto", method.invoke(service, "corto"));
        assertNull(method.invoke(service, (Object) null));
    }

    @Test
    void nowUtc_devuelveFechaNoNula() throws Exception {
        Method method = EmisionEntradasService.class.getDeclaredMethod("nowUtc");
        method.setAccessible(true);
        Object resultado = method.invoke(service);
        assertNotNull(resultado);
    }

    @Test
    void intentarConReintentosInmediatos_primerFalla_segundoOk() {
        Compra compra = compraAprobada();
        compra.setEmisionIntentos(0);
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
        when(detalleReservaRepository.findByReservaIdReserva(ID_RESERVA))
                .thenReturn(List.of(detalle()));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(entradasClient.emitir(any()))
                .thenThrow(new RuntimeException("falló primero"))
                .thenReturn(new ArrayList<>());

        service.intentarConReintentosInmediatos(ID_COMPRA);

        assertEquals(EmisionEntradasService.EMISION_EMITIDA, compra.getEmisionEstado());
        assertEquals(Integer.valueOf(2), compra.getEmisionIntentos());
        verify(entradasClient, times(2)).emitir(any());
    }
}
