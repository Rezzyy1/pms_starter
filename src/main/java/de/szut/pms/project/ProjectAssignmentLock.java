package de.szut.pms.project;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verhindert gleichzeitige Änderungen an Zuordnungen und Projektzeiträumen.
 * Die Sperre muss vor dem Laden der Projekte innerhalb einer Transaktion gesetzt werden.
 */
@Component
public class ProjectAssignmentLock {

    private final JdbcTemplate jdbcTemplate;

    public ProjectAssignmentLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lock() {
        // PostgreSQL gibt die Sperre bei Commit oder Rollback automatisch frei.
        // Normale Lesezugriffe bleiben möglich, weitere Änderungen müssen warten.
        jdbcTemplate.execute("LOCK TABLE project_employees IN SHARE ROW EXCLUSIVE MODE");
    }
}
