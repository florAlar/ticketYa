# Mongo init — TicketYa (msvc-preferencias)

Script montado en `docker-entrypoint-initdb.d` de `db-mongo-preferencias`.
Solo corre en **primer arranque** del volumen vacío.

## Archivo

| DB | Script |
|---|---|
| preferencias | `preferencias/init.js` |

Modelo: `Backend/specs/diagramas/modelo-mongodb.md`.

## Colecciones

- `preferencias_clientes` — un doc por cliente (`idCliente` único)
- `interacciones_eventos` — un doc por interacción

## Índices

- `preferencias_clientes.idCliente` unique
- `interacciones_eventos.idCliente`
- `interacciones_eventos.idEvento`
- `interacciones_eventos (idCliente, fechaEvento desc)`

## Seed (refs lógicas MySQL)

| Concepto | UUID |
|---|---|
| User demo@ | `b0000000-0000-4000-8000-000000000001` |
| Evento Indie | `d0000000-0000-4000-8000-000000000001` |
| Evento Hamlet | `d0000000-0000-4000-8000-000000000002` |

## Re-aplicar

```bash
cd Backend
docker compose stop db-mongo-preferencias
docker compose rm -f db-mongo-preferencias
docker volume rm backend_preferencias_data
docker compose up -d db-mongo-preferencias
```
