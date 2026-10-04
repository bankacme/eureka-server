package com.bank.eureka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Registro: un cliente se da de alta por la API REST de Eureka (lo mismo que hace
 * {@code spring-cloud-starter-netflix-eureka-client} al arrancar un servicio), aparece en el
 * registro y, al darse de baja (lo que hace el cliente al detenerse), desaparece.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationTest {

    private static final String APP = "TEST-CLIENT";
    private static final String INSTANCE_ID = "test-client:9999";
    private static final String INSTANCE = """
            {"instance": {
              "instanceId": "test-client:9999",
              "hostName": "localhost",
              "app": "TEST-CLIENT",
              "ipAddr": "127.0.0.1",
              "status": "UP",
              "port": {"$": 9999, "@enabled": "true"},
              "dataCenterInfo": {
                "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                "name": "MyOwn"
              }
            }}
            """;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void aRegisteredInstanceIsListedAndDisappearsWhenItCancels() {
        ResponseEntity<Void> registered = rest.exchange("/eureka/apps/" + APP, HttpMethod.POST,
                new HttpEntity<>(INSTANCE, jsonHeaders()), Void.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> listed = getApp();
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).contains(INSTANCE_ID).contains("\"UP\"");

        ResponseEntity<Void> cancelled = rest.exchange("/eureka/apps/" + APP + "/" + INSTANCE_ID,
                HttpMethod.DELETE, new HttpEntity<>(jsonHeaders()), Void.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(getApp().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> getApp() {
        return rest.exchange("/eureka/apps/" + APP, HttpMethod.GET, new HttpEntity<>(jsonHeaders()),
                String.class);
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
