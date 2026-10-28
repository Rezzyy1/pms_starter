package de.szut.pms.employee;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EmployeeClient {

    private final RestClient restClient;

    public EmployeeClient(
            @Value("${employee-service.base-url}") String baseUrl) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean existsById(Long employeeId, String token) {

        try {
            var response = restClient.get()
                    .uri("/employees/{id}", employeeId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .toBodilessEntity();

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Unerwartete Antwort des Employee-Service."
                );
            }

            return true;

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 404) {
                return false;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Der Employee-Service hat die Anfrage abgelehnt."
            );

        } catch (ResourceAccessException exception) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Der Employee-Service ist nicht erreichbar."
            );
        }
    }
}
