package de.szut.pms.hello;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fachlich bedeutungslose Beispiel-Entität. Sie zeigt das Muster
 * (Entity → Record-DTOs → Mapper → Repository → Service → Controller →
 * Exception → ApiExceptionHandler), nimmt aber keine Entscheidung über euer
 * eigenes Domänenmodell vorweg — das ist Teil eurer Sprint-Planung.
 */
@Entity
@Table(name = "hello")
@Getter
@Setter
@NoArgsConstructor
public class HelloEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    public HelloEntity(String message) {
        this.message = message;
    }
}
