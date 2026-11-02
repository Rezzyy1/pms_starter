package de.szut.pms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    private final ServletContext context;

    public OpenApiConfiguration(ServletContext context) {
        this.context = context;
    }

    @Bean
    public OpenAPI projectManagementOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addServersItem(new Server().url(context.getContextPath()))
                .info(new Info()
                        .title("Project-Management-Service")
                        .description("""
                                ## Authentifizierung

                                Dieser Service prüft JWTs gegen den lokal per `docker compose` laufenden \
                                Authentik-Server. Einen Bearer-Token bekommst du wie folgt:

                                1. `docker compose up` laufen lassen.
                                2. Die Anfrage in `GetBearerToken.http` ausführen (App-Passwort für `john` \
                                ist dort schon fest hinterlegt, per Blueprint vorprovisioniert).
                                3. `access_token` aus der Antwort kopieren.

                                Details und ein lauffähiges Beispiel stehen in `GetBearerToken.http` im \
                                Projektwurzelverzeichnis.
                                """)
                        .version("0.1"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }
}
