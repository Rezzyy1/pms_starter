package de.szut.pms.hello;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

/**
 * Beispiel-Endpunkte, abgesichert über Authentik (siehe
 * {@link de.szut.pms.security.AuthentikSecurityConfig}). Ersetze diesen
 * Controller durch die Ressourcen, die ihr in eurer Sprint-Planung selbst
 * entworfen habt — das Muster (DTO rein, Service, DTO raus) bleibt gleich.
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    private final HelloService service;

    public HelloController(HelloService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<HelloDto> create(@RequestBody @Valid CreateHelloDto dto) {
        HelloDto created = service.create(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<HelloDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/findByMessage")
    public ResponseEntity<List<HelloDto>> findAllByMessage(@RequestParam String message) {
        return ResponseEntity.ok(service.findAllByMessage(message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
