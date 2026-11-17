package preferencias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import preferencias.document.PreferenciasCliente;
import preferencias.document.NotificacionesPreferencias;
import preferencias.document.RangoPrecio;
import preferencias.dto.MatchingClientesRequestDTO;
import preferencias.dto.NotificacionesPreferenciasDTO;
import preferencias.dto.PreferenciaRequestDTO;
import preferencias.dto.PreferenciaResponseDTO;
import preferencias.repository.PreferenciasClienteRepository;

@ExtendWith(MockitoExtension.class)
class PreferenciaServiceImplTest {

    @Mock
    private PreferenciasClienteRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PreferenciaServiceImpl service;

    @Test
    void obtener_sinDocumento_devuelveDefaults() {
        when(repository.findByIdCliente("cli-1")).thenReturn(Optional.empty());

        PreferenciaResponseDTO dto = service.obtener("cli-1");

        assertEquals("cli-1", dto.getIdCliente());
        assertTrue(dto.getNotificaciones().getPush());
        assertTrue(dto.getNotificaciones().getEmail());
        assertTrue(dto.getNotificaciones().getTipos().isEmpty());
    }

    @Test
    void actualizar_creaDocumentoYGuardaNotificaciones() {
        when(repository.findByIdCliente("cli-1")).thenReturn(Optional.empty());
        when(repository.save(any(PreferenciasCliente.class)))
                .thenAnswer(i -> i.getArgument(0));

        PreferenciaRequestDTO request = new PreferenciaRequestDTO();
        request.setArtistas(List.of("Metallica", " Metallica ", ""));
        request.setCiudades(List.of("Buenos Aires"));
        NotificacionesPreferenciasDTO prefs = new NotificacionesPreferenciasDTO();
        prefs.setPush(false);
        prefs.setTipos(List.of("EventoCancelado"));
        request.setNotificaciones(prefs);

        PreferenciaResponseDTO dto = service.actualizar("cli-1", request);

        assertEquals(List.of("Metallica"), dto.getArtistas());
        assertEquals(List.of("Buenos Aires"), dto.getCiudades());
        assertFalse(dto.getNotificaciones().getPush());
        assertEquals(List.of("EventoCancelado"), dto.getNotificaciones().getTipos());
    }

    @Test
    void actualizar_rangoSinMinimoNiMaximo_limpiaElRango() {
        PreferenciasCliente doc = new PreferenciasCliente();
        doc.setIdCliente("cli-1");
        RangoPrecio anterior = new RangoPrecio();
        anterior.setMin(new java.math.BigDecimal("20000"));
        anterior.setMax(new java.math.BigDecimal("300000"));
        doc.setRangoPrecio(anterior);
        when(repository.findByIdCliente("cli-1")).thenReturn(Optional.of(doc));
        when(repository.save(any(PreferenciasCliente.class)))
                .thenAnswer(i -> i.getArgument(0));

        PreferenciaRequestDTO request = new PreferenciaRequestDTO();
        request.setRangoPrecio(new RangoPrecio());

        PreferenciaResponseDTO dto = service.actualizar("cli-1", request);

        assertNull(dto.getRangoPrecio());
    }

    @Test
    void actualizarArtistas_conservaCategoriasYCiudades() {
        PreferenciasCliente doc = new PreferenciasCliente();
        doc.setIdCliente("cli-1");
        doc.setCategorias(List.of("RECITAL"));
        doc.setCiudades(List.of("Buenos Aires"));
        doc.setArtistas(List.of("Viejo"));
        when(repository.findByIdCliente("cli-1")).thenReturn(Optional.of(doc));
        when(repository.save(any(PreferenciasCliente.class)))
                .thenAnswer(i -> i.getArgument(0));

        PreferenciaResponseDTO dto = service.actualizarArtistas(
                "cli-1", List.of(" Metallica ", "metallica", ""));

        assertEquals(List.of("Metallica", "metallica"), dto.getArtistas());
        assertEquals(List.of("RECITAL"), dto.getCategorias());
        assertEquals(List.of("Buenos Aires"), dto.getCiudades());
    }

    @Test
    void actualizar_notificacionesParcialesNoReseteaOtrosCanales() {
        PreferenciasCliente doc = new PreferenciasCliente();
        doc.setIdCliente("cli-1");
        NotificacionesPreferencias actuales = new NotificacionesPreferencias();
        actuales.setPush(false);
        actuales.setEmail(true);
        actuales.setTipos(List.of("EventoCancelado"));
        doc.setNotificaciones(actuales);
        when(repository.findByIdCliente("cli-1")).thenReturn(Optional.of(doc));
        when(repository.save(any(PreferenciasCliente.class)))
                .thenAnswer(i -> i.getArgument(0));

        PreferenciaRequestDTO request = new PreferenciaRequestDTO();
        NotificacionesPreferenciasDTO parcial = new NotificacionesPreferenciasDTO();
        parcial.setEmail(false);
        request.setNotificaciones(parcial);

        PreferenciaResponseDTO dto = service.actualizar("cli-1", request);

        assertFalse(dto.getNotificaciones().getPush());
        assertFalse(dto.getNotificaciones().getEmail());
        assertEquals(List.of("EventoCancelado"), dto.getNotificaciones().getTipos());
    }

    @Test
    void actualizarNotificaciones_conservaCanalesNoEnviados() {
        PreferenciasCliente doc = new PreferenciasCliente();
        doc.setIdCliente("cli-1");
        when(repository.findByIdCliente("cli-1")).thenReturn(Optional.of(doc));
        when(repository.save(any(PreferenciasCliente.class)))
                .thenAnswer(i -> i.getArgument(0));

        NotificacionesPreferenciasDTO request = new NotificacionesPreferenciasDTO();
        request.setEmail(false);

        NotificacionesPreferenciasDTO dto = service.actualizarNotificaciones("cli-1", request);

        assertTrue(dto.getPush());
        assertFalse(dto.getEmail());
    }

    @Test
    void clientesInteresados_sinArtistas_noConsultaMongo() {
        MatchingClientesRequestDTO request = new MatchingClientesRequestDTO();
        request.setCiudad("Buenos Aires");
        request.setArtistas(List.of());

        assertTrue(service.clientesInteresados(request).isEmpty());
        verify(mongoTemplate, never()).find(any(org.springframework.data.mongodb.core.query.Query.class),
                any(Class.class));
    }

    @Test
    void clientesInteresados_devuelveIdsSinDuplicados() {
        MatchingClientesRequestDTO request = new MatchingClientesRequestDTO();
        request.setCiudad("Buenos Aires");
        request.setArtistas(List.of("Metallica", "metallica"));
        PreferenciasCliente doc = new PreferenciasCliente();
        doc.setIdCliente("cli-1");
        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class),
                any(Class.class))).thenReturn(List.of(doc));

        List<String> ids = service.clientesInteresados(request);

        assertEquals(List.of("cli-1"), ids);
    }
}
