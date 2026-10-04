# eureka-server

Registro y descubrimiento de servicios del sistema bancario (P2, paso 2.1). Ficha:
`bank-docs/services/eureka-server.md`.

- Puerto **8761**. Panel: `http://localhost:8761/` · registro: `http://localhost:8761/eureka/apps`.
- Una sola instancia, sin pares ni autopreservación (demo). No se registra a sí mismo.
- Servlet (Tomcat), no WebFlux: el servidor de Eureka no funciona sobre WebFlux.
- Toda la configuración llega del Config Server (`bank-config/eureka-server.yml`); el
  `application.yml` local solo tiene el nombre y `spring.config.import`.

## Clientes

Cada servicio de negocio trae `spring-cloud-starter-netflix-eureka-client` y toma su configuración
del `application.yml` común de `bank-config`:

| Propiedad | Valor | Para qué |
|---|---|---|
| `eureka.client.service-url.defaultZone` | `${EUREKA_URL:http://localhost:8761/eureka}` | En Docker: `http://eureka-server:8761/eureka` |
| `eureka.client.healthcheck.enabled` | `true` | El estado en el panel sigue a `/actuator/health` (si cae Mongo → `DOWN`) |
| `eureka.client.webclient.enabled` | `true` | Transporte WebClient (los servicios son WebFlux); el servidor lo pone en `false` |
| `eureka.instance.instance-id` | `${spring.application.name}:${server.port}` | Identifica cada instancia en el panel |
| `lease-renewal` / `lease-expiration` | 5 s / 15 s | Una instancia caída desaparece en 15–20 s, no en 90 s |

En las pruebas de cada servicio `eureka.client.enabled: false` (en `src/test/resources/application.yml`).

## Estado
- [x] Servidor con `@EnableEurekaServer`, configuración en `bank-config/eureka-server.yml`.
- [x] Pruebas: arranque (panel, `/eureka/apps`, salud) y alta/baja de una instancia por la API REST
  de Eureka (`RegistrationTest`), que es lo que hace el cliente al arrancar y al detenerse.
- [x] `customer-service`, `account-service`, `credit-service` y `transaction-service` se registran.
- [ ] Diagramas de la ficha (§9): despliegue y secuencia registro → latidos → descubrimiento.
- [ ] Dockerfile (paso 2.7, junto con el resto de servicios).

## Comandos
- Compilar, estilo, pruebas y cobertura: `.\mvnw verify`
- Arrancar (necesita `config-server` arriba): `.\mvnw spring-boot:run`

## Orden de arranque
1. Mongo (`bank-platform`) · 2. `config-server` (8888) · 3. `eureka-server` (8761) ·
4. los servicios, en cualquier orden. Cada uno aparece en el panel en unos 5 s.
