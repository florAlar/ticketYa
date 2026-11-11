# Colección Postman — TicketYa E1

Colección **Postman v2.1** con el environment `TicketYa local` para probar la API de E1 a través del API Gateway.

| Archivo | Contenido |
|---------|-----------|
| `TicketYa-E1.postman_collection.json` | 161 requests en 12 carpetas, con tests en español, captura de IDs y auto-login |
| `TicketYa-local.postman_environment.json` | Variables del entorno local (`baseUrl`, tokens, IDs seed y capturados) |

**Última corrida verificada (2026-11-11):** 161 requests, 466 assertions, 0 fallos (stack local completo con el gateway en `TICKETYA_JWT_ENABLED=false`; las carpetas 10 y 11 corren sin reiniciar datos).

## Requisitos

- Stack levantado desde `Backend/`: `docker compose up -d` (gateway 8080, msvc 8081–8084 y sus bases).
- Datos seed cargados (los `init.sql` de `infrastructure/` corren solo sobre volúmenes vacíos).
- Verificación rápida: `curl http://localhost:8080/` debe responder `api gateway ok!`.

## Cómo usarla

### En Postman

1. Importar los dos archivos `.json`.
2. Elegir el environment **TicketYa local**.
3. Ejecutar la colección con el Runner **en orden, carpetas 00 → 11**. Las carpetas dependen de los IDs que capturan las anteriores (evento, reserva, compra, entradas, devoluciones).

### Por consola (sin instalar nada)

```bash
cd Backend/docs/postman
docker run --rm --network host -v "$PWD":/etc/newman postman/newman \
  run TicketYa-E1.postman_collection.json \
  -e TicketYa-local.postman_environment.json
```

Newman se usa solo para verificar; no es una dependencia del repo. El aviso `Using "atob" is deprecated` que imprime Newman es inocuo.

## Autenticación

No hace falta loguearse a mano. Un script *pre-request* de la colección hace login contra `POST /api/auth/login` cuando el token está vacío o vence en menos de 60 s, y guarda un token por rol:

| Variable | Usuario seed | Rol |
|----------|--------------|-----|
| `token` | `demo@ticketya.com` / `demo1234` | CLIENTE |
| `token_org` | `org@ticketya.com` / `org1234` | ORGANIZADOR |
| `token_admin` | `admin@ticketya.com` / `admin1234` | ADMIN |
| `token_acceso` | `acceso@ticketya.com` / `acceso1234` | PERSONAL_ACCESO |

Cada request declara en su descripción el rol que usa y el status esperado.

**Entrega 1 vs Entrega 2 (flag del gateway `TICKETYA_JWT_ENABLED`):** la colección (hasta la carpeta 09: 127 requests, 376 assertions, 0 fallos) pasó con el flag en `false` (default de E1, identificación por encabezado) y también con `true` (JWT + roles); la carpeta 10 se verificó solo con `false`. Como la colección se loguea y envía `Bearer`, el gateway valida el token en ambos modos. Para probar a mano en modo E1 sin login basta con enviar `X-User-Id` y `X-User-Role`, por ejemplo:

```bash
curl -H 'X-User-Id: b0000000-0000-4000-8000-000000000001' -H 'X-User-Role: CLIENTE' http://localhost:8080/api/entradas
```

## Carpetas

| Carpeta | Requests | Cobertura |
|---------|----------|-----------|
| 00 Auth | 9 | login de los 4 roles, registro, refresh, logout, 401 y 409 |
| 01 Usuarios | 9 | perfil propio, listado, estado, roles; 403 y 404 |
| 02 Eventos | 15 | catálogo con filtros, detalle, borrador → edición → precio → participantes → publicación → reprogramación, borrado |
| 03 Lugares y participantes | 17 | CRUD de lugares, sectores (el numerado se crea con `filas` y `asientosPorFila`), asientos virtuales y participantes, coordenadas opcionales del lugar (latitud/longitud: alta, 400 si falta una o está fuera de rango, edición, `quitarCoordenadas` y detalle de evento) |
| 04 Ventas | 21 | disponibilidad, cupos, reserva (idempotente, renovación, confirmación, cancelación), compra, pago APROBAR y RECHAZAR |
| 05 Entradas | 4 | emisión idempotente, listado, detalle y QR |
| 06 Devoluciones | 10 | solicitud, listado, aprobación, rechazo, reintegro por cancelación |
| 07 Negativos | 17 | 401, 403, 400, 404 y 409 |
| 08 Gateway | 8 | health, rutas públicas sin token y preflight CORS |
| 09 Asientos numerados | 23 | reserva de un asiento concreto (Platea numerada del evento seed), `asientosOcupados` en disponibilidad (RESERVADO → VENDIDO), errores 409/400, entrada con fila y número, devolución que libera el asiento y pago rechazado que libera el asiento |
| 10 Control de acceso QR | 19 | cliente compra una entrada y el personal de acceso la valida: ingreso aceptado, QR repetido (YA_USADA), QR de otro evento, QR inexistente, roles y auditoría de intentos |
| 11 Reportes del organizador | 9 | reporte completo, ventas, sectores, ocupación y recaudación como organizador dueño y ADMIN (con las identidades aritméticas: vigentes y neta), 403 para cliente y personal de acceso, 401 sin token y 404 |

## Flujo de negocio que recorre

publicar evento (abre cupos) → reservar → confirmar → comprar (`Idempotency-Key` obligatoria) → pagar → entradas emitidas → QR → devolución → aprobación/rechazo.

## Variables

- **Fijas (seed):** `baseUrl`, `entradasDirectUrl`, IDs de usuarios, evento publicado y cancelado, lugar, sectores y participante.
- **Capturadas por scripts:** `reservaId`, `compraId`, `pagoId`, `entradaId`, `entrada2Id`, `devolucionId`, entre otras. Se llenan solas al correr la colección en orden.
- **`idempotencyKey`:** se genera una nueva en cada request que la necesita.

## Brechas conocidas de E1 (documentadas, no ocultas)

1. **`POST /eventos/{id}/reintegro-cancelacion` no está ruteado en el gateway.** Por eso 06.09 y 06.10 llaman directo a `msvc-entradas` (`entradasDirectUrl`, puerto 8084) con los headers `X-User-*`, y 07.15 verifica el 404 que devuelve el gateway.
2. **Un pago RECHAZADO no se puede reintentar.** Al rechazarse, la reserva se cancela y el cupo se libera, así que `POST /pagos/{id}/reintentos` responde 409. 07.13 deja constancia de esto.

## Carpeta 09 — asientos numerados

Se puede correr varias veces sin reiniciar los datos: registra un **cliente nuevo por corrida** (evita el límite de 6 entradas por cliente y evento) y elige asientos libres al azar en la Platea del evento seed (100 filas × 250). Variables que usa: `sectorNumeradoSeedId`, `sectorGeneralSeedId` (Campo, general) y `token_asientos`; el resto las captura sola (`asientoNum`, `asientoNum2`, `asientoNum3`, `asientoReservaId`, `asientoCompraId`, `asientoEntradaId`, `asientoDevolucionId`...). Va al final de la colección para no alterar el orden de las carpetas anteriores.

## Carpeta 10 — control de acceso QR (E2-ACC-01)

Autónoma y repetible: registra un **cliente nuevo por corrida**, compra una entrada en el Campo del evento seed y la escanea como `PERSONAL_ACCESO`. Verifica `POST /eventos/{id}/validaciones` (200 `VALIDA`; 409 con `resultado` = `YA_USADA` / `OTRO_EVENTO`; 404 y 400), que la entrada del cliente queda `USADA` y sin QR, y que `GET /eventos/{id}/validaciones` (ADMIN u ORGANIZADOR dueño) lista los tres intentos. Cliente y organizador no pueden validar (403).

## Reiniciar los datos

La colección crea y modifica datos (eventos, lugares, usuarios, reservas, compras, entradas, devoluciones) y cambia el perfil y los roles del usuario demo. Si una corrida falla por datos acumulados (por ejemplo, límite de 6 entradas por cliente y evento, o cupos agotados), reiniciar desde `Backend/`:

```bash
docker compose down -v && docker compose up -d
```

Es destructivo: borra los volúmenes y los `init.sql` vuelven a sembrar los datos.