# e2e-tests — escenarios end-to-end

Módulo Maven independiente con los escenarios **F1–F5** de `E2-TST-01` (Nivel 2). Habla
**solo con el API Gateway** (`http://localhost:8080/api`): no levanta contextos Spring ni usa
Testcontainers. La infraestructura (MySQL, MongoDB, Redis, RabbitMQ y MailHog) la administra
Docker Compose.

## Requisitos

- Docker accesible sin `sudo` y Maven 3.9+ con JDK 17.
- Stack limpio: los escenarios crean sus propios eventos y clientes, pero F2 usa la devolución
  seed pendiente.

## Ejecución

```bash
cd Backend
./scripts/e2e.sh
```

El script hace `down -v`, `up -d --build` del gateway y los 6 microservicios, espera los
healthchecks, ejecuta `mvn -f e2e-tests/pom.xml -Pe2e verify` y limpia al final (incluso si
falla). Opciones: `--keep` y argumentos Maven como `-Dit.test=CompraConfirmadaE2E`.

Sin el perfil `e2e`, `mvn test` compila los tests pero no los ejecuta.

## Escenarios

| Clase | Flujo |
| --- | --- |
| `CompraConfirmadaE2E` | ventas → entradas → notificaciones → preferencias |
| `DevolucionAprobadaE2E` | entradas → ventas → notificaciones |
| `EventoNotificacionesE2E` | eventos → notificaciones |
| `EventoPublicadoE2E` | eventos → preferencias → notificaciones |
| `ReporteOrganizadorE2E` | ventas → entradas + eventos |

Detalle de assertions, datos seed y solución de problemas en
[`../docs/testing/integracion-msvc.md`](../docs/testing/integracion-msvc.md).
