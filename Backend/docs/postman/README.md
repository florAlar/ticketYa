# Colección Postman — TicketYa E1

Colección **Postman v2.1** con el environment `TicketYa local` para probar la API de E1 a través del API Gateway.

| Archivo | Contenido |
|---------|-----------|
| `TicketYa-E1.postman_collection.json` | 104 requests en 9 carpetas, con tests en español, captura de IDs y auto-login |
| `TicketYa-local.postman_environment.json` | Variables del entorno local (`baseUrl`, tokens, IDs seed y capturados) |

**Última corrida verificada:** 104 requests, 307 assertions, 0 fallos (stack local completo, datos seed limpios).

## Requisitos

- Stack levantado desde `Backend/`: `docker compose up -d` (gateway 8080, msvc 8081–8084 y sus bases).
- Datos seed cargados (los `init.sql` de `infrastructure/` corren solo sobre volúmenes vacíos).
- Verificación rápida: `curl http://localhost:8080/` debe responder `api gateway ok!`.

## Cómo usarla

### En Postman

1. Importar los dos archivos `.json`.
2. Elegir el environment **TicketYa local**.
3. Ejecutar la colección con el Runner **en orden, carpetas 00 → 08**. Las carpetas dependen de los IDs que capturan las anteriores (evento, reserva, compra, entradas, devoluciones).

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

## Carpetas

| Carpeta | Requests | Cobertura |
|---------|----------|-----------|
| 00 Auth | 9 | login de los 4 roles, registro, refresh, logout, 401 y 409 |
| 01 Usuarios | 9 | perfil propio, listado, estado, roles; 403 y 404 |
| 02 Eventos | 15 | catálogo con filtros, detalle, borrador → edición → precio → participantes → publicación → reprogramación, borrado |
| 03 Lugares y participantes | 11 | CRUD de lugares, sectores (el numerado se crea con `filas` y `asientosPorFila`), asientos virtuales y participantes |
| 04 Ventas | 21 | disponibilidad, cupos, reserva (idempotente, renovación, confirmación, cancelación), compra, pago APROBAR y RECHAZAR |
| 05 Entradas | 4 | emisión idempotente, listado, detalle y QR |
| 06 Devoluciones | 10 | solicitud, listado, aprobación, rechazo, reintegro por cancelación |
| 07 Negativos | 17 | 401, 403, 400, 404 y 409 |
| 08 Gateway | 8 | health, rutas públicas sin token y preflight CORS |

## Flujo de negocio que recorre

publicar evento (abre cupos) → reservar → confirmar → comprar (`Idempotency-Key` obligatoria) → pagar → entradas emitidas → QR → devolución → aprobación/rechazo.

## Variables

- **Fijas (seed):** `baseUrl`, `entradasDirectUrl`, IDs de usuarios, evento publicado y cancelado, lugar, sectores y participante.
- **Capturadas por scripts:** `reservaId`, `compraId`, `pagoId`, `entradaId`, `entrada2Id`, `devolucionId`, entre otras. Se llenan solas al correr la colección en orden.
- **`idempotencyKey`:** se genera una nueva en cada request que la necesita.

## Brechas conocidas de E1 (documentadas, no ocultas)

1. **`POST /eventos/{id}/reintegro-cancelacion` no está ruteado en el gateway.** Por eso 06.09 y 06.10 llaman directo a `msvc-entradas` (`entradasDirectUrl`, puerto 8084) con los headers `X-User-*`, y 07.15 verifica el 404 que devuelve el gateway.
2. **Un pago RECHAZADO no se puede reintentar.** Al rechazarse, la reserva se cancela y el cupo se libera, así que `POST /pagos/{id}/reintentos` responde 409. 07.13 deja constancia de esto.
3. **Sin requests de asientos todavía** *(2026-11-10)*. Desde la incorporación de asientos virtuales, 03.05/03.06 crean y actualizan el sector numerado con `filas` + `asientosPorFila` y 03.07 espera asientos calculados (`id` nulo, primer código `A-1`). Faltan requests de reserva y compra con `asientos`, la verificación de `asientosOcupados` en disponibilidad y el 409 de asiento tomado. La última corrida verificada (104 requests / 307 assertions) es **anterior** a este cambio y no se re-ejecutó (Docker apagado): reejecutar la colección antes de darla por válida.

## Reiniciar los datos

La colección crea y modifica datos (eventos, lugares, usuarios, reservas, compras, entradas, devoluciones) y cambia el perfil y los roles del usuario demo. Si una corrida falla por datos acumulados (por ejemplo, límite de 6 entradas por cliente y evento, o cupos agotados), reiniciar desde `Backend/`:

```bash
docker compose down -v && docker compose up -d
```

Es destructivo: borra los volúmenes y los `init.sql` vuelven a sembrar los datos.