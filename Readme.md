# Starter für das LF08-Projekt: Project-Management-Service

## Requirements
* Docker https://docs.docker.com/get-docker/
* Docker Compose (bei Windows und Mac schon in Docker enthalten) https://docs.docker.com/compose/install/

## Endpunkt
```
http://localhost:8080
```

## Swagger
```
http://localhost:8080/swagger
```

Nach dem Start von `PmsApplication` stehen dort die Endpunkte für Projekte,
Projektmitarbeiter und die Projektliste eines Mitarbeiters bereit.
Das OpenAPI-Dokument im JSON-Format ist unter
[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) erreichbar.

### JWT-Authentifizierung

In OpenAPI ist die Sicherheitsdefinition `bearerAuth` als HTTP Bearer mit dem Format JWT
hinterlegt. Sie gilt für alle neun dokumentierten REST-Endpunkte; Swagger zeigt diese
als geschützt an und bietet dafür die Schaltfläche **Authorize** an.

Zum Aufrufen der REST-Endpunkte über Swagger:

1. Mit `GetBearerToken.http` einen aktuellen Token beziehen.
2. In Swagger auf **Authorize** klicken und `access_token` einfügen.
   Nur den Token eintragen, ohne Anführungszeichen und ohne `Bearer` davor.
3. Den Endpunkt auswählen und über **Try it out** und **Execute** aufrufen.

Swagger setzt `Authorization: Bearer <access_token>` automatisch.
Verwendet den Wert von `access_token`, nicht `id_token` oder `refresh_token`.
Ein Request enthält damit beispielsweise folgende Header, wobei `<access_token>`
durch den tatsächlichen Token ersetzt wird:

```http
GET /projects HTTP/1.1
Host: localhost:8080
Authorization: Bearer <access_token>
Accept: application/json
```

Die Dokumentation ist ohne Token zugänglich; die REST-Endpunkte benötigen einen gültigen JWT.
Die API prüft dessen Signatur, Aussteller und Gültigkeitszeitraum. Alle authentifizierten
Benutzer haben dieselben Zugriffsrechte.

Fehlt der Token, ist er ungültig oder abgelaufen, antwortet die API mit **401 Unauthorized**,
dem Header `WWW-Authenticate` und einer Fehlerbeschreibung als `application/problem+json`.
In diesem Fall mit `GetBearerToken.http` einen neuen Access-Token beziehen und unter
**Authorize** eintragen. Bei bestehender Autorisierung zuerst **Logout** wählen und
anschließend den neuen Token einfügen. Swagger erneuert Tokens nicht automatisch.

### Dokumentierte Endpunkte

| Methode | Pfad | Funktion | Erfolgsstatus | Fehlercodes |
|---|---|---|---|---|
| GET | `/projects` | Alle Projekte mit ihren Mitarbeitern abrufen | 200 | 400, 401 |
| POST | `/projects` | Ein Projekt anlegen | 201 | 400, 401, 404, 502, 503 |
| GET | `/projects/{id}` | Ein Projekt abrufen | 200 | 400, 401, 404 |
| PUT | `/projects/{id}` | Ein Projekt ändern | 204 | 400, 401, 404, 409, 502, 503 |
| DELETE | `/projects/{id}` | Ein Projekt löschen | 204 | 400, 401, 404 |
| GET | `/projects/{projectId}/employees` | Mitarbeiter eines Projekts abrufen | 200 | 400, 401, 404 |
| POST | `/projects/{projectId}/employees` | Einen Mitarbeiter zuordnen | 201 | 400, 401, 404, 409, 502, 503 |
| DELETE | `/projects/{projectId}/employees/{employeeId}` | Eine Mitarbeiterzuordnung entfernen | 200 | 400, 401, 404 |
| GET | `/employees/{employeeId}/projects` | Projekte eines Mitarbeiters mit dessen Qualifikation abrufen | 200 | 400, 401, 404, 502, 503 |

Swagger beschreibt die Pfadparameter, die JSON-Modelle der Anfragen und Antworten
sowie die HTTP-Fehlercodes. Fehler werden als `application/problem+json` zurückgegeben.
Antworten mit Status 204 enthalten keinen Body.

### Aufrufe über Swagger prüfen

Voraussetzung: Die Docker-Dienste und `PmsApplication` laufen. Einen gültigen JWT wie oben
beschrieben unter **Authorize** eintragen. Für die Prüfung einen eigenen Testdatensatz
verwenden und die vom POST zurückgegebene Projekt-ID für die weiteren Aufrufe übernehmen.
Mitarbeiter-ID und Qualifikation müssen im Employee-Service vorhanden sein; dessen
Swagger-Oberfläche ist unter `http://localhost:8089/swagger` erreichbar.
Der Testzeitraum darf sich mit keinem anderen Projekt des Mitarbeiters überschneiden.

Die folgenden Aufrufe jeweils mit **Try it out** und **Execute** ausführen:

| Schritt | Aufruf | Erwartetes Ergebnis |
|---|---|---|
| 1 | POST `/projects` mit vollständigem Request-JSON | 201, Projekt mit erzeugter `id` |
| 2 | GET `/projects` | 200, das Testprojekt ist in der Liste enthalten |
| 3 | GET `/projects/{id}` | 200, Daten des Testprojekts |
| 4 | PUT `/projects/{id}` mit geändertem Namen und vollständigem Request-JSON | 204 ohne Body; ein erneutes GET zeigt die Änderung |
| 5 | GET `/projects/{projectId}/employees` | 200, zunächst eine leere Mitarbeiterliste |
| 6 | POST `/projects/{projectId}/employees` mit gültiger Mitarbeiter-ID und Qualifikation | 201; ein erneutes GET der Mitarbeiterliste enthält die Zuordnung |
| 7 | GET `/employees/{employeeId}/projects` | 200, Mitarbeiter-ID und Projektliste mit ID, Name, Startdatum, Enddatum und Qualifikation |
| 8 | DELETE `/projects/{projectId}/employees/{employeeId}` | 200 mit Mitarbeiter-ID und Qualifikation; die Zuordnung fehlt anschließend in beiden Listen |
| 9 | DELETE `/projects/{id}` | 204 ohne Body; ein erneutes GET des Projekts liefert 404 |

Zusätzlich prüfen: Ohne Autorisierung antworten alle neun Endpunkte mit 401. Vor Schritt 6
eine falsche Qualifikation angeben: erwartet wird 400. Direkt nach Schritt 6 dieselbe
Zuordnung erneut senden: erwartet wird 409. Direkt nach Schritt 8 das Entfernen
wiederholen: erwartet wird 404.

`OpenApiDocumentationTest` prüft automatisiert die öffentliche Swagger-Oberfläche,
ihre Konfiguration, alle neun Operationen einschließlich der Bearer-Sicherheitsanforderung
sowie die drei erforderlichen JSON-Request-Modelle. Die Tests verwenden eine separate
PostgreSQL-Datenbank über Testcontainers und können als Testklasse in der IDE gestartet werden.
Die Bedienung der Schaltflächen im Browser ist zusätzlich anhand der obigen Schritte zu prüfen.

### Request-Daten

`POST /projects` verwendet `CreateProjectDto`, `PUT /projects/{id}` verwendet
`UpdateProjectDto`. Beide Anfragen enthalten alle folgenden Felder.
Beim Ändern wird die Projekt-ID über den Pfad angegeben.

| Feld | JSON-Typ | Bedeutung und Einschränkungen |
|---|---|---|
| `name` | String | Projektbezeichnung, nicht leer, höchstens 255 Zeichen |
| `responsibleEmployeeId` | Integer (int64) | Positive ID des verantwortlichen Mitarbeiters |
| `customerId` | Integer (int64) | Positive ID des beauftragenden Kunden |
| `customerContactName` | String | Ansprechpartner beim Kunden, nicht leer, höchstens 255 Zeichen |
| `comment` | String | Projektziel; beim Anlegen nicht leer, beim Ändern ist `""` erlaubt |
| `startDate` | String (date) | Erster Projekttag im Format `YYYY-MM-DD` |
| `endDate` | String (date) | Letzter Projekttag im Format `YYYY-MM-DD`, nicht vor `startDate` |

Beispiel für das Anlegen oder Ändern eines Projekts:

```json
{
  "name": "Kundenportal",
  "responsibleEmployeeId": 1,
  "customerId": 2,
  "customerContactName": "Anna Müller",
  "comment": "Entwicklung eines Kundenportals",
  "startDate": "2026-11-01",
  "endDate": "2026-12-15"
}
```

`POST /projects/{projectId}/employees` verwendet `AddProjectEmployeeDto`:

```json
{
  "employeeId": 11,
  "qualification": "Java"
}
```

Beide Felder sind Pflichtfelder. `employeeId` muss positiv sein.
`qualification` darf nicht leer sein und höchstens 255 Zeichen enthalten.
Sie muss exakt zu einem `skill` des Mitarbeiters im Employee-Service passen,
einschließlich Groß- und Kleinschreibung.
Die IDs und die Qualifikation in den Beispielen sind durch vorhandene Werte zu ersetzen.
GET- und DELETE-Anfragen benötigen keinen JSON-Body.

### Response-Daten

| Verwendung | Modell und Inhalt |
|---|---|
| POST `/projects`, GET `/projects/{id}` | `ProjectDto`: die sieben Projektdaten sowie `id` und `employees` |
| GET `/projects` | Array von `ProjectDto`; ohne Projekte `[]` |
| GET `/projects/{projectId}/employees` | `ProjectEmployeesDto`: `projectId`, `projectName`, `employees` |
| Mitarbeiter zuordnen oder entfernen | `ProjectEmployeeDto`: `employeeId`, `qualification` |
| GET `/employees/{employeeId}/projects` | `EmployeeProjectsDto`: einmal `employeeId` und die Liste `projects` |

Ein vollständiges Projekt enthält seine Mitarbeiter mit deren Qualifikation im Projekt:

```json
{
  "id": 1,
  "name": "Kundenportal",
  "responsibleEmployeeId": 1,
  "customerId": 2,
  "customerContactName": "Anna Müller",
  "comment": "Entwicklung eines Kundenportals",
  "startDate": "2026-11-01",
  "endDate": "2026-12-15",
  "employees": [
    { "employeeId": 11, "qualification": "Java" }
  ]
}
```

Die Projektliste eines Mitarbeiters enthält pro Projekt ein `EmployeeProjectDto`:

```json
{
  "employeeId": 11,
  "projects": [
    {
      "id": 1,
      "name": "Kundenportal",
      "startDate": "2026-11-01",
      "endDate": "2026-12-15",
      "qualification": "Java"
    }
  ]
}
```

Ohne Zuordnungen werden `employees` beziehungsweise `projects` als `[]` ausgegeben.
Die Qualifikation stammt jeweils aus der Zuordnung zum konkreten Projekt.
PUT `/projects/{id}` und DELETE `/projects/{id}` antworten mit 204 ohne JSON-Body.

### Bedeutung der Statuscodes

Die Tabelle der Endpunkte nennt die vorgesehenen Erfolgs- und Fehlerantworten.

| Code | HTTP-Status | Bedeutung |
|---|---|---|
| 200 | OK | Daten wurden abgerufen oder eine Mitarbeiterzuordnung wurde entfernt; die Antwort enthält JSON. Leere Listen sind ebenfalls erfolgreich. |
| 201 | Created | Ein Projekt oder eine Mitarbeiterzuordnung wurde angelegt; die Antwort enthält die angelegten Daten als JSON. |
| 204 | No Content | Ein Projekt wurde geändert oder gelöscht; die Antwort enthält keinen Body. |
| 400 | Bad Request | Ungültiges JSON, ungültige Eingabefelder, eine nicht als `Long` lesbare Pfad-ID oder ein ungültiger Projektzeitraum. Auch eine nicht vorhandene Mitarbeiterqualifikation oder eine ungültige Bearer-Authentifizierungsanfrage (`invalid_request`) führen zu 400. |
| 401 | Unauthorized | Der Bearer-Token fehlt, ist ungültig oder abgelaufen. Die Antwort enthält auch den Header `WWW-Authenticate`. |
| 404 | Not Found | Das angefragte Projekt, der geprüfte Mitarbeiter oder Kunde wurde nicht gefunden, oder die zu entfernende Mitarbeiterzuordnung existiert nicht. |
| 409 | Conflict | Der Mitarbeiter ist dem Projekt bereits zugeordnet oder im angefragten Zeitraum einem anderen Projekt zugeordnet. Das gilt auch beim Ändern der Projektdaten für bereits zugeordnete Mitarbeiter. |
| 502 | Bad Gateway | Der Employee-Service liefert einen unerwarteten HTTP-Status oder bei der Qualifikationsprüfung eine nicht auswertbare Antwort. |
| 503 | Service Unavailable | Der Employee-Service ist nicht erreichbar oder antwortet nicht innerhalb des Zeitlimits. |

Für einzelne Endpunkte gelten folgende Besonderheiten:

- `POST /projects` meldet einen unbekannten verantwortlichen Mitarbeiter oder Kunden mit **404**.
  `PUT /projects/{id}` meldet eine geänderte, unbekannte Mitarbeiter- oder Kunden-ID mit **400**;
  ein unbekanntes Projekt führt weiterhin zu **404**.
- Die Kundenprüfung verwendet derzeit `DummyCustomerClient`. Dieser akzeptiert jede positive,
  durch die Request-Validierung zugelassene Kunden-ID. Die Fehlerantwort für einen unbekannten
  Kunden ist für die spätere echte Kundenprüfung vorgesehen.
- Eine numerische, aber nicht vorhandene Projekt-ID führt zu **404**; eine nicht lesbare ID
  wie `abc` führt zu **400**. Bei `GET /employees/{employeeId}/projects` muss die Mitarbeiter-ID
  zusätzlich positiv sein, andernfalls wird **400** zurückgegeben.
- Das erneute Löschen eines bereits entfernten Projekts oder einer bereits entfernten
  Mitarbeiterzuordnung führt zu **404**.
- Start- und Enddatum zählen bei der Prüfung auf Überschneidungen mit. Auch wenn ein Projekt
  am Enddatum eines anderen Projekts desselben Mitarbeiters beginnt, entsteht **409**.
- Bei `GET /projects` bezieht sich **400** auf eine ungültige Bearer-Authentifizierungsanfrage.
  Die übrigen Endpunkte können diesen Authentifizierungsfehler ebenfalls zurückgeben.

### Fehlerantworten

Fehler enthalten `type`, `title`, `status`, `detail` und `instance`.
Bei der Validierung von Request-Feldern kommt zusätzlich eine `errors`-Map
mit Feldnamen und Meldungen hinzu, zum Beispiel:

```json
{
  "type": "about:blank",
  "title": "Ungültige Eingabe",
  "status": 400,
  "detail": "Die Anfrage enthält ungültige Felder.",
  "instance": "/projects",
  "errors": {
    "responsibleEmployeeId": "Die Mitarbeiter-ID muss positiv sein."
  }
}
```

# Abhängigkeiten starten

`docker compose up` startet alles, was dieser Service braucht:

| Dienst | Wofür | Port |
|---|---|---|
| `postgres-pms` | eure eigene Datenbank | `5433` (extern) |
| `postgres-employee` + `employee` | der bestehende Employee-Service | `5432` / `8089` |
| `postgres-authentik` + `redis` + `authentik-server` + `authentik-worker` | der zentrale Identity-Provider | `9000` / `9443` |

```bash
docker compose up -d
```

Achtung: Die Container laufen dauerhaft! Wenn sie nicht mehr benötigt werden, sollten sie gestoppt werden.

### Stoppen
```bash
docker compose down
```

### Datenbank wipen, z. B. bei Problemen
```bash
docker compose down
docker volume rm starter_2026_pms_postgres_data
docker compose up
```
(Der genaue Volume-Name hängt vom Ordnernamen ab — `docker volume ls` zeigt ihn an.)

### Intellij-Ansicht für die eigene Postgres-Datenbank einrichten
```
1. Container laufen lassen (docker compose up)
2. im Ordner resources die Datei application.properties öffnen und die URL der Datenbank kopieren
3. rechts im Fenster den Reiter Database öffnen
4. In der Database-Symbolleiste auf das Datenbanksymbol mit dem Schlüssel klicken
5. auf das Pluszeichen klicken
6. Datasource from URL auswählen
7. URL der DB einfügen und PostgreSQL-Treiber auswählen, mit OK bestätigen
8. Username pms und Passwort secret eintragen (siehe application.properties), mit Apply bestätigen
9. im Reiter Schemas alle Häkchen entfernen und lediglich vor pms_db und public Häkchen setzen
10. mit Apply und OK bestätigen
```

# Employee-Service

Der bestehende Employee-Service läuft lokal mit, als fertiges Docker-Image. Seine eigene
Swagger-Dokumentation:
```
http://localhost:8089/swagger
```

Ruft ihn über `employee-service.base-url` aus `application.properties` auf (`http://localhost:8089`),
nicht über eine feste Adresse im Code — dann bleibt die Adresse an einer Stelle änderbar.

# Kunden-Service

Der Kunden-Service existiert noch nicht — er wird parallel von einem anderen Team gebaut. Das
Package `customer` enthält dafür schon eine Schnittstelle (`CustomerClient`) mit einer
Platzhalter-Implementierung (`DummyCustomerClient`), die jede Kennung als gültig behandelt.
Tauscht die Implementierung aus, sobald der echte Service bereitsteht — an den Aufrufstellen
ändert sich dadurch nichts.

# Authentik

Authentik läuft ebenfalls lokal mit. Beim ersten Start richtet ein Blueprint
(`authentik_blueprints/zzz_employee_setup.yaml`) automatisch einen Benutzer `john`, den
OIDC-Provider `employee_api` und ein App-Passwort für `john` ein — denselben Provider, gegen
den auch der Employee-Service prüft. Ein einziger Login genügt für alle Backoffice-Services
(Single Sign-on).

### Admin-Oberfläche
```
http://localhost:9000
```
Login: `a@b.com` / `secret` (nur nötig, wenn ihr selbst in Authentik nachschauen wollt —
für den JWT-Token unten nicht erforderlich).

### JWT-Token holen
Kein manueller Schritt in Authentik nötig — das App-Passwort für `john` ist im Blueprint fest
vorgegeben (`teachme123`) und steht schon in [GetBearerToken.http](GetBearerToken.http).
Einfach den Request darin ausführen und `access_token` aus der Antwort kopieren.

Ausführliche Beispiel-Anfragen stehen in [GetBearerToken.http](GetBearerToken.http) und
[SampleRequests.http](SampleRequests.http).

### Projekte mit einem gültigen JWT abrufen

[GetProjects.http](GetProjects.http) enthält Anfragen für alle Projekte, ein einzelnes Projekt
und dessen Mitarbeiter. Zuerst in `GetBearerToken.http` einen Token holen und den Wert von
`access_token` beim jeweiligen Prompt einsetzen. Die Anfrage sendet ihn als
`Authorization: Bearer <access_token>` an den PMS.

`GET /projects` antwortet bei gültigem JWT mit `200 OK`, auch wenn die Projektliste leer ist.
Für die beiden Einzelabfragen muss `project_id` auf eine vorhandene Projekt-ID gesetzt werden.
Ein unbekanntes Projekt liefert trotz gültigem JWT `404 Not Found`.

### Berechtigungen

Alle authentifizierten Benutzer haben im PMS dieselben Zugriffsrechte. Jeder Benutzer mit
einem gültigen JWT darf Projekte abrufen, anlegen, ändern und löschen sowie Projektmitarbeiter
einsehen. Rollen und Scopes im JWT schränken diese Zugriffsrechte in dieser Version nicht ein.
Die Sicherheitskonfiguration verwendet dafür `authenticated()`.

# Wo ihr weiterarbeitet

* `hello` — Beispiel-Ressource, die das Muster zeigt (Entity → Record-DTOs → Mapper →
  Repository → Service → Controller → Exception → `ApiExceptionHandler`). Fachlich bedeutungslos
  — ersetzt sie durch eure eigenen, in der Sprint-Planung entworfenen Ressourcen.
* `security/AuthentikSecurityConfig` — trägt eure Endpunkt-Pfade in `authorizeHttpRequests()` ein,
  sobald sie existieren. Der Rest (JWT-Prüfung, CORS) steht bereits.
* `config/OpenApiConfiguration` — hier passt ihr Titel/Beschreibung der Swagger-Dokumentation an.
* `customer/CustomerClient` — Dummy, siehe oben.

## Gemeinsames Modell für Projektmitarbeiter

`ProjectEmployeeEntity` speichert in `project_employees` die Zuordnung eines Mitarbeiters zu
einem Projekt: Projekt, Mitarbeiter-ID und die im Projekt eingesetzte Qualifikation.
Die Kombination aus Projekt-ID und Mitarbeiter-ID ist eindeutig.

`ProjectEntity.employees` enthält die Zuordnungen. Mit `addEmployee` werden beide Seiten der
JPA-Beziehung gesetzt; beim Speichern des Projekts werden seine Zuordnungen mitgespeichert:

```java
project.addEmployee(employeeId, qualification);
projectRepository.save(project);
```

Vor einer Zuweisung muss der spätere Team-Service die Mitarbeiter-ID, die Qualifikation und
zeitliche Überschneidungen prüfen. `addEmployee` stellt die JPA-Beziehung her; die fachlichen
Prüfungen gehören zur Story „Projektteam verwalten“.

`GET /projects` und `GET /projects/{id}` geben die Zuordnungen als `employees` mit `employeeId`
und `qualification` aus. Ohne Zuordnungen ist die Liste leer. `ProjectRepository` lädt die
Zuordnungen über `@EntityGraph` zusammen mit den Projekten.

Beim Löschen eines Projekts werden seine Zuordnungen durch JPA mitgelöscht. Ein aus
`employees` entfernter Eintrag wird durch `orphanRemoval` ebenfalls gelöscht.
