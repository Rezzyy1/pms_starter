package de.szut.pms.hello;

import jakarta.validation.constraints.Size;

public record CreateHelloDto(

        @Size(min = 3, message = "Die Nachricht muss mindestens 3 Zeichen lang sein.")
        String message) {
}
