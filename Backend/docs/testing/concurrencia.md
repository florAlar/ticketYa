# Informe de concurrencia — msvc-ventas (E1)

> **Paso:** P4.3 · **Fecha de la corrida:** 2026-11-09 · **Alcance:** último cupo de un sector (D-16)
> **Commits:** P4.1 `2d5ec31` (unitario) · P4.2 `21faa97` (integración)

## 1. Objetivo

Verificar que el mecanismo de reserva de `msvc-ventas` **no vende de más** cuando varios clientes compiten por la última unidad de un sector.

El mecanismo real es un **lock pesimista de fila** sobre `cupo_sector`: la reserva lee el cupo con `SELECT ... FOR UPDATE` (`CupoSectorRepository.findByEventoAndSectorForUpdate`), valida disponibilidad y actualiza los contadores dentro de la misma transacción.

El criterio de aceptación es **no sobreventa**: nunca más de una reserva sobre el último cupo, y nunca contadores negativos. No hay meta de performance bloqueante.

## 2. Entorno

| Ítem | Valor |
|------|-------|
| Host | Lenovo V15 G3, Ubuntu, Docker 29.8.2 (Ubuntu 24.04.5 LTS), 15.716 MB de RAM |
| Build | imagen `maven:3.9-eclipse-temurin-17` (Maven 3.9, Java 17.0.20.1) |
| Framework | Spring Boot 4.1.2, Hibernate 7.4.13 |
| Test runners | Surefire 3.5.6 (unitarios) · Failsafe 3.5.6 (integración) |
| Base de datos del IT | `mysql:8.4` (8.4.11) con Testcontainers 2.0.5, aislamiento `REPEATABLE_READ` |
| Redis / Feign | no participan (el IT trabaja sobre el repositorio; el unitario los mockea) |

Comandos (desde la raíz del repo):

```bash
# Unitarios (incluye ReservaConcurrenciaTest), sin Docker
docker run --rm -v "$HOME/.m2:/root/.m2" -v "$PWD/Backend":/w -w /w/msvc-ventas \
  maven:3.9-eclipse-temurin-17 \
  mvn -B -ntp -Dmaven.repo.local=/root/.m2/repository test

# Integración con MySQL real (perfil -Pit)
docker run --rm --network host \
  -v "$HOME/.m2:/root/.m2" -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -v "$PWD/Backend":/w -w /w/msvc-ventas \
  maven:3.9-eclipse-temurin-17 \
  mvn -B -ntp -Dmaven.repo.local=/root/.m2/repository -Pit verify
```

## 3. Escenarios

### 3.1 Unitario — `ReservaConcurrenciaTest` (P4.1, build normal)

| Aspecto | Detalle |
|---------|---------|
| Clase bajo prueba | `VentaServiceImpl.crearReserva` (servicio real) |
| Estado inicial | 1 cupo de sector: capacidad 1, disponible 1, reservada 0, vendida 0 |
| Concurrencia | 20 hilos (`ExecutorService` + `CountDownLatch` de arranque simultáneo), cada uno reserva 1 unidad con un cliente distinto |
| Lock | emulado con un `ReentrantLock` justo: se toma en `findByEventoAndSectorForUpdate` y se libera en `save` (commit) o al salir con excepción (rollback) |
| Dependencias | repositorios, Feign, `StockCache`, mapper y emisión mockeados; sin Spring ni base de datos |

**Aserciones:** 0 errores inesperados; exactamente **1 éxito** y **19** `BusinessConflictException`; disponible = 0, reservada = 1, vendida = 0.

**Resultado:** verde, 4,183 s.

### 3.2 Integración — `CupoSectorConcurrenciaIT` (P4.2, perfil `-Pit`)

| Aspecto | Detalle |
|---------|---------|
| Clase bajo prueba | `CupoSectorRepository.findByEventoAndSectorForUpdate` sobre MySQL real |
| Contexto | `@DataJpaTest` con datasource del contenedor `mysql:8.4`; esquema generado por Hibernate (`ddl-auto=create-drop`) |
| Estado inicial | 1 cupo de sector: capacidad 1, disponible 1, reservada 0, vendida 0 |
| Concurrencia | **2 transacciones reales**: la primera toma el lock de fila y lo retiene 600 ms; la segunda se lanza cuando la primera ya tiene el lock |
| Qué se verifica | la segunda queda bloqueada en el `SELECT ... FOR UPDATE`; al continuar ve disponible = 0 y falla con `BusinessConflictException` |

**Aserciones:**

- exactamente 1 éxito y 1 conflicto;
- la segunda transacción esperó al menos 300 ms (prueba que realmente se bloqueó);
- fila final: disponible = 0 (no negativo), reservada = 1, vendida = 0, capacidad = 1.

**Resultado:** verde, 21,94 s en total. Ese tiempo incluye ~8,6 s de arranque del contenedor MySQL y ~8,1 s de arranque del contexto Spring.

**Evidencia en el log SQL:** dos `select ... where id_evento=? and id_sector=? for update` y un único `update cupo_sector` (el de la transacción ganadora). La perdedora no llega a actualizar.

### 3.3 Integración — `AsientoReservadoConcurrenciaIT` (SEAT-06, perfil `-Pit`)

Mismo patrón que 3.2, aplicado al **mismo asiento** de un sector numerado y sobre el **esquema real**.

| Aspecto | Detalle |
|---------|---------|
| Clases bajo prueba | `CupoSectorRepository.findByEventoAndSectorForUpdate` y `AsientoReservadoRepository` sobre MySQL real |
| Contexto | `@DataJpaTest` con `mysql:8.4`; el contenedor carga `infrastructure/mysql/ventas/init.sql` y Hibernate solo valida (`ddl-auto=validate`). Así entra la clave única `uk_asiento_evento_sector_fila_numero`, que solo existe en `init.sql` |
| Estado inicial | 1 cupo de sector (capacidad 200) y reservas sin asientos; se limpian los datos seed |

| Caso | Qué verifica | Resultado esperado |
|---|---|---|
| `mismoAsiento_unaTransaccionGanaOtraEsperaVeAsientoTomadoYFalla` | 2 transacciones reales piden el asiento C-14: la primera retiene el lock 600 ms; la segunda espera, ve el asiento tomado y falla con `BusinessConflictException` | 1 éxito, 1 conflicto, espera ≥ 300 ms, 1 fila en `asiento_reservado`, cupo 199/1 |
| `mismoAsiento_sinLockLaClaveUnicaDeMySqlRechazaElDuplicado` | 8 hilos insertan el mismo asiento **sin lock ni pre-chequeo**: solo actúa la base | 1 éxito, 7 `DataIntegrityViolationException`, 1 fila |
| `conflictoDeClaveUnica_haceRollbackDeLosContadoresDelCupo` | una transacción descuenta el cupo y luego choca con la clave única | los contadores quedan como antes (disponible 199, reservada 1) |
| `asientosDistintos_ambasReservasProsperan` | dos asientos distintos no generan falsos conflictos | 2 éxitos, cupo 198/2 |

**Prueba de que no es vacuo:** quitando temporalmente la clave única de `init.sql`, fallan los dos casos que la ejercitan (se insertan 8 filas del mismo asiento y no hay rollback); los otros dos siguen verdes porque dependen del lock y del pre-chequeo.

## 4. Resultados

| Escenario | Hilos / transacciones | Éxitos | Conflictos | Sobreventa | Contadores negativos |
|-----------|----------------------|--------|------------|------------|----------------------|
| Unitario (lock emulado) | 20 | 1 | 19 | no | no |
| IT cupo (MySQL 8.4 real) | 2 | 1 | 1 | no | no |
| IT mismo asiento, con lock (MySQL real) | 2 | 1 | 1 | no | no |
| IT mismo asiento, solo clave única (MySQL real) | 8 | 1 | 7 | no | no |

`mvn test` de msvc-ventas: **109 tests, 0 fallos**. `mvn verify -Pit`: **BUILD SUCCESS** (1 test de integración del cupo + 4 del asiento, 0 fallos; corrida del 2026-11-10).

## 5. Métricas de latencia y throughput

**No medidas.** El plan (P4.2) las consideraba informativas y no bloqueantes. El IT solo mide el tiempo de espera de la segunda transacción para asertar que hubo bloqueo (≥ 300 ms), pero no lo reporta; no se calcularon p50, p95 ni throughput.

## 6. Observaciones de la corrida

- Los `WARN`/stack traces de Hibernate `Table 'ventas.compra' doesn't exist` al inicio son esperables: `create-drop` intenta borrar restricciones sobre una base recién creada y vacía. No indican un problema.
- Al cerrar, aparecen `WARN` de HikariCP (`No operations allowed after connection closed`) porque el contenedor se detiene antes de cerrar el pool, y un `[ERROR] Surefire is going to kill self fork JVM` (la JVM tardó más de 30 s en salir tras `System.exit`). El build termina en SUCCESS; es ruido de cierre.
- Ryuk está deshabilitado (`TESTCONTAINERS_RYUK_DISABLED=true`) porque la imagen no está disponible localmente. Si una corrida se interrumpe puede quedar un contenedor `mysql:8.4` huérfano (`docker ps` / `docker rm -f`).

## 7. Limitaciones

1. **Contención baja en la base real.** El IT usa 2 transacciones, no decenas. Prueba que el lock de fila serializa y que el segundo ve el estado actualizado, pero no ejerce alta contención sobre MySQL.
2. **El IT no pasa por `VentaServiceImpl`.** Ejercita el repositorio con la lógica de validación replicada en la prueba. La lógica real del servicio (`crearReserva`) se prueba bajo concurrencia solo en el unitario, con el lock emulado: ese fake no prueba el motor de base de datos.
3. **Esquema generado por Hibernate** en `CupoSectorConcurrenciaIT` (§3.2). El IT de asientos (§3.3) sí usa el `init.sql` real.
4. **Solo reservas sobre un único sector.** No se prueba concurrencia entre compras sobre la misma reserva, ni pagos concurrentes, ni liberación de cupo en devoluciones.
5. **Asiento numerado** *(actualizado 2026-11-10)*: los sectores numerados se venden por asiento concreto (asientos virtuales, tabla `asiento_reservado` con clave única por evento + sector + fila + número). El mismo asiento se prueba de dos formas: unitario con lock emulado (`ReservaAsientoConcurrenciaTest`, 20 hilos: 1 éxito, 19 conflictos) y contra **MySQL real** (`AsientoReservadoConcurrenciaIT`, §3.3: lock de cupo, clave única y rollback de contadores). Igual que en el punto 2, el IT ejercita los repositorios con la lógica de reserva replicada, no `VentaServiceImpl.crearReserva`.

## 8. Diferencias con el plan original (§10 del spec)

| Plan | Implementado |
|------|--------------|
| TST-CONC-01: 50 hilos sobre la última unidad | 20 hilos (suficiente para el escenario; un único cupo) |
| TST-CONC-02: 20 hilos, disponibilidad 5, exactamente 5 exitosas | no implementado (el `Javadoc` del IT usa el ID `TST-CONC-02` para el escenario de 2 transacciones) |
| TST-CONC-03: compras concurrentes sobre la misma reserva | no implementado |
| IT: 50 transacciones sobre el último cupo | 2 transacciones con retención de lock de 600 ms |
| IT con `withInitScript` del init real | esquema por `ddl-auto=create-drop` |
| Medición p50/p95 informativa | no medida |

## 9. Conclusión

Con los escenarios ejecutados no se observó **sobreventa ni contadores negativos**:

- sobre la lógica real del servicio con 20 hilos compitiendo por la última unidad (lock de fila emulado), y
- sobre un `SELECT ... FOR UPDATE` real de MySQL 8.4, donde la segunda transacción esperó el lock y falló con conflicto de cupo.

La cobertura de concurrencia de E1 es acotada (ver §7 y §8) y queda declarada como tal.