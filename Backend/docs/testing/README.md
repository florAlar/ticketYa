# Testing — índice

Documentación de las pruebas del proyecto. Los conteos vigentes y el estado de cada tarea se
mantienen en [`specs/backlog.md`](../../specs/backlog.md); acá se explica **qué se prueba y cómo
correrlo**.

| Documento | Qué cubre |
|---|---|
| [`concurrencia.md`](./concurrencia.md) | No doble venta: unitarios con lock emulado e ITs con MySQL real (cupo y asiento). |
| [`integracion-msvc.md`](./integracion-msvc.md) | Nivel 1 (ITs de borde con Testcontainers) y Nivel 2 (E2E F1–F5 contra el stack). |
| [`notificaciones.md`](./notificaciones.md) | Verificación de eventos → bandeja, PUSH/EMAIL, MailHog e idempotencia. |
| [`carga.md`](./carga.md) | Informe de la matriz JMeter 25/50/100 (compra normal y con fila). |
| [`carga/`](./carga/) | Planes `.jmx`, scripts de usuarios y resultados de la prueba de carga. |

## Comandos

Unitarios (sin Docker), dentro de cada módulo:

```bash
cd Backend/msvc-<nombre>
mvn test
```

ITs de borde con Testcontainers (requiere Docker; módulos con ITs: usuarios, eventos, ventas,
entradas, preferencias y notificaciones):

```bash
cd Backend/msvc-<nombre>
mvn -Pit verify
```

E2E contra el stack completo:

```bash
cd Backend
./scripts/e2e.sh          # limpia volúmenes, levanta, corre -Pe2e verify y limpia
./scripts/e2e.sh --keep   # deja el stack levantado para inspeccionar
```

Prueba de carga:

```bash
cd Backend/docs/testing/carga
./jmeter.sh compra-con-fila.jmx 25
./jmeter.sh compra-normal.jmx 50
```

Colección Postman/Newman:

```bash
cd Backend/docs/postman
# ver README.md para la corrida por :3000 o directo al gateway :8080
```

## Convenciones

- Un test unitario no debe requerir Docker ni red.
- Los ITs usan Testcontainers y requieren Docker. Varias clases usan
  `@Testcontainers(disabledWithoutDocker = true)` y se omiten si no está; las de concurrencia y de
  contexto de aplicación pueden fallar si Docker no está disponible.
- Los E2E hablan **solo** con el API Gateway; en general crean sus propios datos (F2 usa la
  devolución seed pendiente).
- Los datos de prueba viven en `Backend/infrastructure/` y solo se cargan en volúmenes nuevos.
