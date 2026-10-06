# Secuencia — Registro, latidos y descubrimiento en Eureka

Implementado por `eureka-server` (`@EnableEurekaServer`, puerto 8761) y por el cliente de Eureka de
cada servicio (`bank-config/application.yml`). Lo prueban `RegistrationTest` (registro, listado y
baja por REST) y, en vivo, el panel de `http://localhost:8761`.

```mermaid
sequenceDiagram
    autonumber
    participant S as account-service<br/>(cliente Eureka, WebClient)
    participant E as eureka-server :8761
    participant GW as api-gateway<br/>(LoadBalancer, caché 5 s)
    participant T as transaction-service<br/>(WebClient @LoadBalanced)
    actor C as Cliente HTTP

    Note over S: arranca, lee su config de config-server
    S->>E: POST /eureka/apps/ACCOUNT-SERVICE (instance-id, host, puerto, health)
    E-->>S: 204 registrado (estado UP según /actuator/health)

    loop cada 5 s (lease-renewal-interval)
        S->>E: PUT /eureka/apps/ACCOUNT-SERVICE/{instance-id} (latido)
        E-->>S: 200
    end

    loop cada 5 s
        GW->>E: GET /eureka/apps/delta
        T->>E: GET /eureka/apps/delta
        E-->>GW: instancias registradas
        E-->>T: instancias registradas
    end

    C->>GW: GET /api/v1/accounts/{id}
    GW->>GW: lb://account-service → elige instancia (round robin)
    GW->>S: GET /api/v1/accounts/{id}
    S-->>GW: 200
    GW-->>C: 200

    T->>T: http://account-service/api/v1 → elige instancia
    T->>S: POST /accounts/{id}/movements
    S-->>T: 200

    alt el servicio se apaga ordenadamente
        S->>E: DELETE /eureka/apps/ACCOUNT-SERVICE/{instance-id}
    else deja de enviar latidos (caída)
        Note over E: a los 15 s sin latido (lease-expiration) lo quita
    end
    Note over GW,T: en ≤ 5 s su caché ya no lo ofrece (mientras tanto<br/>el circuit breaker responde 503)
```

## Notas

- **Nombres, no puertos.** Los `base-url` de los clientes son `http://<servicio>/api/v1`; el puerto
  real lo da Eureka. Levantar una segunda instancia (otro puerto) la incorpora sola al balanceo.
- **Dos `WebClient.Builder`.** El `@LoadBalanced` es solo para llamar a otros servicios; el cliente
  de Eureka usa el builder normal (`@Primary`) para hablar con `localhost:8761`.
- **Health real.** `eureka.client.healthcheck.enabled=true`: si Mongo cae, la instancia queda `DOWN`
  y deja de recibir tráfico (Redis no cuenta, es solo caché).
- **Sin réplica de Eureka** en el bootcamp (`register-with-eureka=false`, `fetch-registry=false` en el
  servidor); en producción serían dos o tres nodos que se replican.
