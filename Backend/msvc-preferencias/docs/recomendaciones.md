# Recomendaciones y historial — msvc-preferencias

## 1. Ingesta del historial

```
msvc-ventas --(compra.confirmada)--> eventos.exchange
                                        │
                                        ▼
                           preferencias.compras (cola durable + DLQ)
                                        │
                                        ▼
                     CompraConfirmadaListener → InteraccionService
                                        │
                                        ▼
                        interacciones_eventos (MongoDB)
```

- Se reutiliza el evento `compra.confirmada` que ya publica `msvc-ventas` después del
  commit; **no se agregó una llamada REST entre microservicios**.
- El upsert es idempotente por `idCompra`: una reentrega de RabbitMQ no duplica el historial.
- El snapshot del evento (`nombreEvento`, `categoria`, `ciudad`, `artistas[]`,
  `fechaEvento`) se obtiene con Feign a `GET /eventos/{id}`. Si eventos no responde, la
  interacción se guarda igual con snapshot nulo (fail-soft) y se loguea.
- Los mensajes con error van a `preferencias.compras.dlq` (`default-requeue-rejected=false`).

## 2. Recomendaciones

### 2.1 Similares a un evento (público)

`GET /eventos/{id}/recomendaciones?limit=6`

1. Clientes que compraron el evento fuente.
2. Sus otras compras → conteo de co-ocurrencia por evento.
3. Similitud de contenido con el evento fuente: mismo `tipo`, misma `ciudad`,
   artistas en común.
4. Excluye el evento fuente y eventos no publicados/pasados.
5. Fallback: próximos eventos publicados ordenados por alta demanda y fecha.

### 2.2 Personalizadas (JWT)

`GET /recomendaciones/me?limit=10`

1. Perfil explícito (`preferencias_clientes`): categorías, ciudades, artistas, rango de precio.
2. Co-compra: eventos comprados por clientes que compraron los mismos eventos que yo.
3. Excluye eventos ya comprados.
4. Fallback: próximos eventos con `esAltaDemanda` (cold start).

### 2.3 Score

```
score = categoria * [tipo ∈ categorias]
      + ciudad    * [ciudad ∈ ciudades]
      + artista   * [artista ∈ artistas]  (por artista en común)
      + coCompra  * min(coCompras, tope)
      + precio    * [precio dentro del rango]
      + demanda   * [esAltaDemanda]
```

Pesos en `ticketya.recomendaciones.pesos.*`. Desempate por `fechaInicio` ascendente.
El campo `motivo` explica la señal principal (ej. “Porque te gusta Metallica”).

## 3. Historial

`GET /clientes/{id}/historial?page=&size=`

- Dueño (`X-User-Id == id`) o `ADMIN` (`X-User-Roles`).
- Orden `fechaCompra` descendente.
- Se lee del snapshot denormalizado: no hay llamadas a eventos en el request.

## 4. Backfill de compras existentes

Las compras del seed de MySQL se insertaron directo en la base y **no pasaron por
RabbitMQ**. Para reflejarlas:

1. `msvc-ventas` expone `GET /internas/compras` (compras `APROBADA`, con fecha, monto y
   cantidad). Es interno: no está expuesto por el gateway.
2. `msvc-preferencias` expone `POST /internas/backfill`, que importa y upsertea por
   `idCompra`. Idempotente.
3. Script: `scripts/backfill-preferencias.sh`.

## 5. Simplificaciones justificadas

- Co-compra on-the-fly con 2 consultas a Mongo (sin colección precomputada). Suficiente
  para el volumen del TP; se documenta la evolución.
- Pesos fijos, no aprendizaje automático.
- Consistencia eventual: el historial aparece cuando Rabbit entrega el mensaje.
- Solo se ingesta `COMPRA`; el modelo queda preparado para `ASISTENCIA` (validación QR) y
  `VISUALIZACION` (front) sin migración.
- El catálogo de candidatos se limita a 200 eventos publicados (configurable).
