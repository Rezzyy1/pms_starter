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
        String contextPath = context.getContextPath();

        return new OpenAPI()
                .addServersItem(new Server().url(contextPath.isEmpty() ? "/" : contextPath))
                .info(new Info()
                        .title("Project-Management-Service")
                        .description("""
                                REST-API zur Verwaltung von Kundenprojekten und Projektmitarbeitern.
                                Projekte können angelegt, abgerufen, geändert und gelöscht werden.
                                Mitarbeiter werden mit ihrer Qualifikation einem Projekt zugeordnet.
                                Die Projekte eines Mitarbeiters können über dessen ID abgerufen werden.

                                ## Authentifizierung

                                Die REST-Endpunkte benötigen einen gültigen JWT des Authentik-Service.
                                Alle authentifizierten Benutzer haben dieselben Zugriffsrechte.
                                Die Swagger-Oberfläche und `/v3/api-docs` sind ohne Anmeldung zugänglich.
                                Die API prüft Signatur, Aussteller und Gültigkeitszeitraum des Tokens.

                                1. Einen Token mit der Anfrage in `GetBearerToken.http` beziehen.
                                2. Oben auf **Authorize** klicken und den Wert von `access_token` einfügen.
                                3. Nur den Token eintragen, ohne Anführungszeichen und ohne `Bearer` davor.
                                4. Den gewünschten Endpunkt über **Try it out** und **Execute** aufrufen.

                                Swagger ergänzt den Header `Authorization: Bearer <access_token>` automatisch.
                                Die Bearer-Authentifizierung gilt für alle dokumentierten REST-Endpunkte.
                                Als Token wird `access_token` verwendet, nicht `id_token` oder `refresh_token`.

                                Fehlt der Token, ist er ungültig oder abgelaufen, antwortet die API mit
                                **401 Unauthorized** und einer Fehlerbeschreibung als `application/problem+json`.
                                Einen neuen Access-Token über `GetBearerToken.http` beziehen und unter
                                **Authorize** ersetzen, bei bestehender Autorisierung zuerst **Logout** wählen.
                                Swagger erneuert den Token nicht automatisch.
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
                                                .description("Den JWT aus dem Feld access_token der Authentik-Antwort "
                                                        + "einfügen, ohne Anführungszeichen und ohne das Präfix Bearer. "
                                                        + "Bei 401 einen neuen Access-Token beziehen und hier ersetzen.")
                                )
                );
    }
}
