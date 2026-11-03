package de.szut.pms.security;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.szut.pms.project.ProjectEntity;
import de.szut.pms.project.ProjectRepository;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JwtAuthenticationTest extends AbstractIntegrationTest {

    private static final String ISSUER = "https://authentik.test/application/o/employee_api/";
    private static final RSAKey SIGNING_KEY = createSigningKey();
    private static final AtomicInteger jwksRequests = new AtomicInteger();
    private static final HttpServer jwksServer = startJwksServer();

    @Autowired
    private ProjectRepository projectRepository;

    @DynamicPropertySource
    static void authenticationProperties(DynamicPropertyRegistry registry) {
        registry.add("authentik.issuer-uri", () -> ISSUER);
        registry.add("authentik.jwk-set-uri", () ->
                "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks");
    }

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @AfterAll
    static void stopJwksServer() {
        jwksServer.stop(0);
    }

    @Test
    void validSignedJwtAllowsReadingProjects() throws Exception {
        ProjectEntity project = projectRepository.save(new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Entwicklung eines Kundenportals",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15)));

        mockMvc.perform(get("/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(project.getId()))
                .andExpect(jsonPath("$[0].name").value("Kundenportal"));

        assertThat(jwksRequests.get()).isPositive();
    }

    @Test
    void missingJwtIsRejected() throws Exception {
        assertUnauthorized(get("/projects"), "Ein Bearer-Token ist erforderlich.");
    }

    @Test
    void malformedJwtIsRejected() throws Exception {
        assertInvalidTokenIsRejected("not-a-jwt");
    }

    @Test
    void jwtSignedWithAnotherKeyIsRejected() throws Exception {
        Instant now = Instant.now();
        RSAKey untrustedKey = createSigningKey();
        String token = signedToken(untrustedKey, ISSUER, now, now.plusSeconds(300));

        assertInvalidTokenIsRejected(token);
    }

    @Test
    void jwtWithAnotherIssuerIsRejected() throws Exception {
        Instant now = Instant.now();
        String token = signedToken(SIGNING_KEY, "https://other-issuer.test/",
                now, now.plusSeconds(300));

        assertInvalidTokenIsRejected(token);
    }

    @Test
    void expiredJwtIsRejected() throws Exception {
        Instant now = Instant.now();
        String token = signedToken(SIGNING_KEY, ISSUER,
                now.minusSeconds(600), now.minusSeconds(120));

        assertInvalidTokenIsRejected(token);
    }

    private void assertInvalidTokenIsRejected(String token) throws Exception {
        assertUnauthorized(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token),
                "Der Bearer-Token ist ungültig oder abgelaufen.")
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
    }

    private ResultActions assertUnauthorized(
            MockHttpServletRequestBuilder request, String detail) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Authentifizierung fehlgeschlagen"))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/projects"));
    }

    private static RSAKey createSigningKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("pms-test-key").generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Test-Schlüssel konnte nicht erstellt werden.", exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            byte[] jwks = new JWKSet(SIGNING_KEY.toPublicJWK()).toString()
                    .getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/jwks", exchange -> {
                try (exchange) {
                    jwksRequests.incrementAndGet();
                    exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    exchange.sendResponseHeaders(200, jwks.length);
                    exchange.getResponseBody().write(jwks);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("JWKS-Testserver konnte nicht gestartet werden.", exception);
        }
    }

    private String validToken() throws JOSEException {
        Instant now = Instant.now();
        return signedToken(SIGNING_KEY, ISSUER, now, now.plusSeconds(300));
    }

    private String signedToken(
            RSAKey signingKey, String issuer, Instant issuedAt, Instant expiresAt) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("jwt-test-user")
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(issuedAt.minusSeconds(5)))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT).keyID(signingKey.getKeyID()).build(),
                claims);
        token.sign(new RSASSASigner(signingKey));
        return token.serialize();
    }
}
