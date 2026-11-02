package de.szut.pms.customer;

import org.springframework.stereotype.Component;

/**
 * Platzhalter, solange der echte Kunden-Service nicht erreichbar ist:
 * Jede nicht-null Kennung gilt als vorhanden. Tausche diese Klasse gegen
 * eine Implementierung aus, die den Kunden-Service über RestClient
 * abfragt, sobald er bereitsteht — die Schnittstelle {@link CustomerClient}
 * bleibt dabei gleich, du musst also an den Aufrufstellen nichts ändern.
 */
@Component
public class DummyCustomerClient implements CustomerClient {

    @Override
    public boolean existsById(Long customerId) {
        return customerId != null;
    }
}
