# Integración entre microservicios (E2-TST-01)

> **Estado:** implementado y verificado (Niveles 1 y 2) · **Última actualización:** 2026-11-21

Este documento describe cómo se prueban los flujos que cruzan más de un microservicio. La
especificación de trabajo vive en [`specs/integracion-entre-msvc.md`](../../specs/integracion-entre-msvc.md).

## 1. Dos niveles

| Nivel | Qué prueba | Dónde corre | Comando |
| --- | --- | --- | --- |
| **1 — ITs de borde** | Un servicio Spring Boot con MySQL/Rabbit reales y el lado remoto simulado: contratos async, idempotencia, publicación después del commit | Módulo del servicio (`src/test/java`) | `mvn -Pit verify` |
| **2 — E2E de stack** | El despliegue real del compose: gateway → servicios → Feign → Rabbit → bases; efectos observables (entradas, historial, bandeja, MailHog) | `Backend/e2e-tests` | `scripts/e2e.sh` |

El Nivel 1 da diagnósticos precisos y no necesita todo el stack. El Nivel 2 es el que cierra la
brecha “entre microservicios” porque no mockea a los participantes del flujo.

## 2. Nivel 1 — ITs de borde (Testcontainers)

| Test | Servicio | Infra real | Qué verifica |
| --- | --- | --- | --- |
| `NotificacionMensajesConsumeIT` | notificaciones | MySQL 8.4 + RabbitMQ | `evento.*`, `compra.confirmada` y `devolucion.aprobada`; filas PUSH/EMAIL, agrupación por `clave_mensaje` e idempotencia |
| `CompraConfirmadaPublisherIT` | ventas | MySQL 8.4 + RabbitMQ | Publicación de `compra.confirmada` **después del commit** y ausencia de mensaje en rollback |
| `DevolucionAprobadaPublisherIT` | entradas | MySQL 8.4 + RabbitMQ | Publicación de `devolucion.aprobada` después del commit y no publicación en rollback |
| `CompraConfirmadaIngestIT` | preferencias | MongoDB + RabbitMQ | Ingesta idempotente de compras en el historial |

```bash
cd Backend/msvc-notificaciones && mvn -Pit verify
cd Backend/msvc-ventas        && mvn -Pit verify
cd Backend/msvc-entradas      && mvn -Pit verify
cd Backend/msvc-preferencias  && mvn -Pit verify
```

Requieren Docker. Los tests usan `@Testcontainers(disabledWithoutDocker = true)`: sin Docker se
omiten, no fallan.

## 3. Nivel 2 — E2E de stack

### 3.1 Requisitos

- Docker accesible por el usuario (`docker info` funciona sin `sudo`).
- Maven 3.9+ y JDK 17.
- Puertos libres: `8080–8086`, `8025` (MailHog), `15672` (panel Rabbit), `3307–3311` (MySQL),
  `6380` (Redis), `27018` (Mongo).

Si el grupo `docker` no está activo en la sesión:

```bash
newgrp docker                      # o cerrar sesión y volver a entrar
sg docker -c './scripts/e2e.sh'    # alternativa sin reiniciar la sesión
```

### 3.2 Comando

```bash
cd Backend
./scripts/e2e.sh
```

El script:

1. `docker compose down -v` (base limpia; **borra los volúmenes**).
2. `docker compose build` del gateway y los 6 microservicios.
3. Levanta **primero** bases e infraestructura (5 MySQL, Mongo, Redis, RabbitMQ y MailHog) y espera
   a que estén `healthy`. MySQL corre `init.sql` con un servidor temporal sin red; arrancar los 17
   contenedores a la vez hacía que el healthcheck agotara los reintentos.
4. Levanta el gateway y los 6 microservicios con las dependencias ya sanas.
5. Espera los endpoints HTTP de cada servicio.
6. `mvn -f e2e-tests/pom.xml -Pe2e verify` (Failsafe, patrón `**/*E2E.java`).
7. `docker compose down -v` al terminar, incluso si falla.

Opciones:

```bash
./scripts/e2e.sh --keep                          # deja el stack para inspeccionar
./scripts/e2e.sh -Dit.test=CompraConfirmadaE2E    # un escenario (también -Dtest=)
E2E_BASE_URL=http://localhost:8080 ./scripts/e2e.sh
```

Sin el perfil `e2e`, `mvn test` compila pero no ejecuta los E2E.

### 3.3 Escenarios

| Escenario | Flujo | Assertions principales |
| --- | --- | --- |
| `CompraConfirmadaE2E` (F1) | ventas → entradas → notificaciones → preferencias | Compra `APROBADA`; 2 entradas `VENDIDA`; historial con la compra del evento; notificación `CompraConfirmada` EMAIL; correo en MailHog; `noLeidas`; `X-Correlation-Id` |
| `DevolucionAprobadaE2E` (F2) | entradas → ventas → notificaciones | Entrada `DEVUELTA` sin QR; cupo liberado (+1); notificación `DevolucionAprobada` con `canales=["PUSH","EMAIL"]`; una sola entrada en la bandeja; correo en MailHog |
| `EventoNotificacionesE2E` (F3) | eventos → notificaciones | `EventoModificado`, `EventoSuspendido` y `EventoCancelado`, cada uno con PUSH+EMAIL y una sola entrada en la bandeja |
| `EventoPublicadoE2E` (F4) | eventos → preferencias → notificaciones | `NuevoEventoArtistaFavorito` para el cliente demo; contenido con evento y ciudad; cupos abiertos en ventas; correo en MailHog |
| `ReporteOrganizadorE2E` (F5) | ventas → entradas + eventos | Totales y suma por sector; `vigentes = vendidas − devueltas − aReintegrar`; `neta = bruta − devuelta − aReintegrar`; sub-reportes de ventas, ocupación, recaudación y sectores |

Cada escenario crea sus propios datos (evento, cliente, compra) para no depender del orden de
ejecución. F2 usa la devolución seed pendiente; si ya fue procesada, el test se **omite** con un
mensaje que indica resetear los volúmenes.

### 3.4 Datos seed usados

| Recurso | ID |
| --- | --- |
| Evento Metallica | `d0000000-0000-4000-8000-000000000001` |
| Lugar (Buenos Aires) | `c0000000-0000-4000-8000-000000000001` |
| Sector Campo | `c0000000-0000-4000-8000-000000000011` |
| Participante Metallica | `p0000000-0000-4000-8000-000000000001` |
| Devolución pendiente | `aa000000-0000-4000-8000-000000000011` |
| Entrada de esa devolución | `aa000000-0000-4000-8000-000000000001` |

Usuarios: `demo@ticketya.com` / `demo1234`, `org@ticketya.com` / `org1234`,
`admin@ticketya.com` / `admin1234`.

## 4. Fuera de alcance

- **F6 (fila virtual):** escenario opcional, no implementado en esta iteración.
- **Degradación con dependencia caída (F5 → 503):** requiere detener un contenedor a mitad del
  test. Se cubre en los unitarios/controlador del reporte (excepción propia + cuerpo estándar); el
  E2E valida el camino feliz.
- **Carga y throughput:** `E2-TST-03`.
- **Matriz completa de roles:** `E2-TST-02`.
- **UI:** los E2E hablan solo con el gateway.

## 5. Resultados

### Nivel 2 — E2E de stack

| Fecha | Entorno | Comando | Resultado |
| --- | --- | --- | --- |
| 2026-11-21 | Docker 29.10.0 · Linux Mint 22.3 · volúmenes limpios | `./scripts/e2e.sh` | **5 tests, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`) |

Tiempos de la corrida: `ReporteOrganizadorE2E` 70 s, `EventoNotificacionesE2E` 35 s,
`CompraConfirmadaE2E` 34 s, `DevolucionAprobadaE2E` 30 s, `EventoPublicadoE2E` 17 s.

### Nivel 1 — ITs de borde

| Módulo | Comando | Resultado |
| --- | --- | --- |
| `msvc-notificaciones` | `mvn -Pit verify` | `NotificacionMensajesConsumeIT` 5 + `MsvcNotificacionesApplicationIT` 1 → **6 OK** |
| `msvc-ventas` | `mvn -Pit verify` | `CompraConfirmadaPublisherIT` 2 + `FilaRedisStoreIT` 15 + `AsientoReservadoConcurrenciaIT` 4 + `CupoSectorConcurrenciaIT` 1 → **22 OK** |
| `msvc-entradas` | `mvn -Pit verify` | `DevolucionAprobadaPublisherIT` 2 + `ValidacionIngresoConcurrenciaIT` 2 → **4 OK** |
| `msvc-preferencias` | `mvn -Pit verify` | `CompraConfirmadaIngestIT` 1 + `RecomendacionMongoIT` 4 → **5 OK** |

Los ITs requieren Docker y se ejecutan con el grupo activo (`sg docker -c 'mvn -Pit verify'` si la
sesión no lo heredó).

Para la evidencia, adjuntar:

- Salida de Failsafe (`e2e-tests/target/failsafe-reports/`, `<msvc>/target/failsafe-reports/`).
- Logs del gateway y servicios con el mismo `X-Correlation-Id`.
- Captura o consulta a MailHog (`http://localhost:8025`) y a la API de Rabbit
  (`http://localhost:15672`).

## 6. Problemas frecuentes

| Síntoma | Causa / solución |
| --- | --- |
| `permission denied` en `/var/run/docker.sock` | El grupo `docker` no está activo en la sesión: `newgrp docker`, reiniciar sesión o `sg docker -c ...` |
| `port is already allocated` | Otro stack o proceso usa los puertos; `docker compose down -v` y revisar `ss -ltn` |
| `dependency failed to start: container db-mysql-* is unhealthy` | El `init.sql` de MySQL corre con un servidor temporal sin red y tarda; el script levanta las bases primero y espera `healthy`. No arrancar los 17 contenedores juntos ni bajar el `start_period` del healthcheck |
| Los servicios no pasan el healthcheck | `docker compose logs --tail 100 ms-<servicio>`; el primer build puede tardar varios minutos |
| F2 se omite | La devolución seed ya fue procesada en un stack con volúmenes viejos: correr el script sin `--keep` para resetear |
| Timeout esperando notificación o email | El job de envío corre cada 30 s; revisar Rabbit (`notificaciones.*`), MailHog y logs de `ms-notificaciones`. MailHog muestra los asuntos con acentos como palabras MIME (`=?UTF-8?Q?...?=`): el helper `esperarEmail` los decodifica |
