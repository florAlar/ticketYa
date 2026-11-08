package usuarios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI del contexto de usuarios/autenticación (DOC-API-01).
 *
 * <p>No declara un requisito de seguridad global: cada operación protegida lo indica con
 * {@code @SecurityRequirement(name = "bearerAuth")}. Los paths documentados son los internos
 * (sin {@code /api}); el gateway aplica {@code StripPrefix=1}.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI usuariosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa — Usuarios y autenticación")
                        .version("1.0.0")
                        .description("""
                                Identidad, sesión y administración de usuarios de TicketYa.
                                La autenticación y el RBAC los aplica el API Gateway; este microservicio
                                confía en los headers X-User-Id / X-User-Role propagados tras validar el JWT.
                                """)
                        .contact(new Contact()
                                .name("Equipo TicketYa — TUDAi 2026")
                                .email("equipo@ticketya.com.ar"))
                        .license(new License().name("Uso académico — TUDAi 2026")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
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
                                        JWT emitido por POST /auth/login (o /auth/refresh).
                                        El gateway valida la firma y propaga X-User-Id, X-User-Email,
                                        X-User-Role y X-User-Roles a los microservicios.
                                        """)));
    }
}
