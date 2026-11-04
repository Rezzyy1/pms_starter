package de.szut.pms.hello;

public class HelloNotFoundException extends RuntimeException {

    public HelloNotFoundException(Long id) {
        super("Es gibt keinen Eintrag mit der Kennung " + id + ".");
    }
}
