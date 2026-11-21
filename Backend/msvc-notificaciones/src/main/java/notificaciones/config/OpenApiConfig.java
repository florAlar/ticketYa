package notificaciones.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI de notificaciones, bandeja y estado de envío.
 *
 * <p>Los paths son los internos (sin {@code /api}); el gateway aplica
 * {@code StripPrefix=1} y el RBAC. Las operaciones protegidas declaran
 * {@code @SecurityRequirement(name = "bearerAuth")}.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI notificacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa — Notificaciones (Email / Push y Bandeja)")
                        .version("1.0.0")
                        .description("""
                                Bandeja de notificaciones del usuario autenticado (una entrada por \
                                mensaje agrupando PUSH y EMAIL, con contador de no leídas) y \
                                operaciones administrativas de alta manual y consulta de estado de envío.
                                El API Gateway valida el JWT y propaga X-User-Id / X-User-Role.
                                """)
                        .contact(new Contact()
                                .name("Equipo TicketYa — TUDAi 2026")
                                .email("equipo@ticketya.com.ar"))
                        .license(new License().name("Uso académico — TUDAi 2026")))
                .tags(List.of(
                        new Tag()
                                .name("Bandeja")
                                .description("Notificaciones del usuario autenticado (/notificaciones/me)."),
                        new Tag()
                                .name("Administración")
                                .description("Alta administrativa y consulta de estado de envío (solo ADMIN)."),
                        new Tag()
                                .name("Salud")
                                .description("Smoke del microservicio.")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8086")
                                .description("Local — directo al microservicio"),
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local — vía API Gateway (StripPrefix=1)")
                                .extensions(Map.<String, Object>of("x-gateway", true)),
                        new Server()
                                .url("https://ticketya.com.ar/api")
                                .description("Producción — vía API Gateway")
                                .extensions(Map.<String, Object>of("x-gateway", true))))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        JWT emitido por POST /auth/login de msvc-usuarios.
                                        El gateway valida la firma y propaga X-User-Id,
                                        X-User-Email, X-User-Role y X-User-Roles.
                                        """)));
    }
}
