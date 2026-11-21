package notificaciones;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import notificaciones.config.OpenApiConfig;
import notificaciones.controller.NotificacionController;
import notificaciones.repository.NotificacionRepository;
import notificaciones.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato OpenAPI en runtime, sin base de datos. */
@WebMvcTest(controllers = NotificacionController.class)
@Import(OpenApiConfig.class)
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class NotificacionOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    @MockitoBean
    private NotificacionRepository notificacionRepository;

    @Test
    void exponeOpenApiConLosEndpointsDeNotificaciones() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("TicketYa — Notificaciones (Email / Push y Bandeja)"))
                .andExpect(jsonPath("$.paths['/notificaciones/me']").exists())
                .andExpect(jsonPath("$.paths['/notificaciones/me/{id}']").exists())
                .andExpect(jsonPath("$.paths['/notificaciones/me/{id}/leida']").exists())
                .andExpect(jsonPath("$.paths['/notificaciones/{id}/estado']").exists())
                .andExpect(jsonPath("$.paths['/notificaciones'].post").exists())
                .andExpect(jsonPath("$.paths['/notificaciones'].get").exists())
                .andExpect(jsonPath("$.paths['/'].get").exists())
                .andExpect(jsonPath("$.tags[0].name").value("Bandeja"))
                .andExpect(jsonPath("$.tags[1].name").value("Administración"))
                .andExpect(jsonPath("$.tags[2].name").value("Salud"))
                .andExpect(jsonPath("$.paths['/notificaciones/me'].get.tags[0]").value("Bandeja"))
                .andExpect(jsonPath("$.paths['/notificaciones/me'].get.operationId")
                        .value("listarPropias"))
                .andExpect(jsonPath("$.paths['/notificaciones'].post.operationId")
                        .value("crearAdministrativa"))
                .andExpect(jsonPath("$.paths['/notificaciones/{id}/estado'].get.operationId")
                        .value("estadoEnvio"))
                .andExpect(jsonPath(
                        "$.paths['/notificaciones/me'].get.responses['200']"
                                + ".content['application/json']").exists())
                .andExpect(jsonPath(
                        "$.paths['/'].get.responses['200'].content['text/plain']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}
