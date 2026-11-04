package de.szut.pms.customer;

/**
 * Zugriff auf den Kunden-Service. Der echte Service existiert noch nicht —
 * er wird parallel von einem anderen Team gebaut. Bis dahin übernimmt
 * {@link DummyCustomerClient} diese Rolle.
 */
public interface CustomerClient {

    /**
     * Prüft, ob eine Kundennummer existiert. Wirf bei einer unbekannten
     * Kennung eine eigene fachliche Ausnahme, sobald ihr das an der
     * Aufrufstelle braucht — dieselbe Vorgehensweise wie bei der Prüfung
     * einer Mitarbeiter-Id gegen den Employee-Service.
     */
    boolean existsById(Long customerId);
}
