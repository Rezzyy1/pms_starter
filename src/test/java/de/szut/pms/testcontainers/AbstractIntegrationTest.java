package de.szut.pms.testcontainers;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import de.szut.pms.hello.HelloRepository;

/**
 * Startet einen echten Postgres-Container über Testcontainers und die volle
 * Anwendung. Zum Authentifizieren in Tests nicht Authentik ansprechen —
 * dafür gibt es {@code @WithMockUser} (siehe Tests im Package hello).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresContextInitializer.class)
public class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected HelloRepository helloRepository;

    @BeforeEach
    void setUp() {
        helloRepository.deleteAll();
    }
}
