# Trazabilidad y bitácora (E2-NFR-03)

Cómo se sigue una operación a través de los microservicios y qué queda registrado de las acciones críticas.

## Correlation id

Cada request lleva un identificador en la cabecera `X-Correlation-Id`:

1. **El API Gateway lo genera** (un UUID) si el cliente no manda uno. Si el cliente manda uno válido (8 a 64 caracteres de `A-Z a-z 0-9 . _ -`) se respeta, por ejemplo para que el front o Postman marquen su propia traza. Uno inválido (corto, largo, con espacios o saltos de línea) se **descarta** y se genera otro: así no se inyecta texto arbitrario en los logs.
2. El gateway lo **reenvía a los servicios** y lo **devuelve en la respuesta** (`X-Correlation-Id`, una sola vez).
3. Cada servicio lo guarda en el **MDC** de la request, por lo que aparece en **cada línea de log**: `[nombre-del-servicio,correlation-id]`.
4. Las llamadas **Feign** entre servicios lo **propagan** (interceptor `FeignCorrelationConfig`).

Los servicios (`usuarios`, `eventos`, `ventas`, `entradas`, `preferencias`, `notificaciones`) y el gateway tienen la misma clase `CorrelationIdFilter` en su paquete `observability`, con tests (`CorrelationIdTest`).

### Línea de acceso por request

Cada servicio escribe una línea `trace.http` por request con método, ruta, estado y duración (no se registran `/actuator`, `/v3/api-docs` ni `/swagger-ui`):

```
INFO [api-gateway,pago-trace-0001] ... trace.http : POST /api/compras/e43d…/pagos -> 201 (640 ms)
INFO [msvc-ventas,pago-trace-0001]  ... trace.http : POST /compras/e43d…/pagos -> 201 (627 ms)
```

## Bitácora de operaciones críticas

Las acciones importantes se registran con el logger `bitacora` (una línea, formato `clave=valor`). El correlation id sale en el prefijo de la línea y el usuario se toma del `X-User-Id` que inyecta el gateway ya verificado.

| Servicio | Acciones |
|---|---|
| usuarios | `LOGIN_OK`, `LOGIN_FALLIDO` (credenciales o cuenta desactivada), `REGISTRO_OK` |
| eventos | `EVENTO_CREADO`, `EVENTO_PUBLICADO`, `EVENTO_ESTADO_<ESTADO>` (suspendido, cancelado…) |
| ventas | `PAGO_APROBADO`, `PAGO_RECHAZADO`, `RESERVAS_EXPIRADAS`, `REPORTE_CONSULTADO` |
| entradas | `ENTRADAS_EMITIDAS`, `DEVOLUCION_SOLICITADA` / `_APROBADA` / `_RECHAZADA`, `REINTEGRO_CANCELACION`, `INGRESO_VALIDADO` / `INGRESO_RECHAZADO` |

Ejemplo:

```
INFO [msvc-ventas,pago-trace-0001]    ... bitacora : accion=PAGO_APROBADO usuario=b0000000-… recurso=e43d6198-… detalle=pago=… monto=95000.00 proveedor=MOCK_PASARELA
INFO [msvc-entradas,pago-trace-0001]  ... bitacora : accion=ENTRADAS_EMITIDAS usuario=b0000000-… recurso=f0000000-… detalle=evento=d0000000-… cantidad=1
```

No se registran contraseñas ni tokens. Los saltos de línea de los valores se reemplazan por espacios para que un dato no pueda fabricar una línea falsa de bitácora.

## Cómo seguir una operación

1. Mandar la request con un id propio (o tomar el de la respuesta):

   ```bash
   curl -H "X-Correlation-Id: mi-traza-0001" -H "Authorization: Bearer $TOKEN" \
        -X POST http://localhost:8080/api/compras/$COMPRA/pagos -d '{"resultado":"APROBAR"}' -H 'Content-Type: application/json'
   ```

2. Buscar ese id en todos los contenedores:

   ```bash
   docker compose logs ms-gateway ms-ventas ms-entradas ms-eventos ms-usuarios | grep mi-traza-0001
   ```

   Se ve el recorrido completo (gateway → ventas → entradas) y las líneas `bitacora` de lo que se decidió en cada paso.

3. Solo la bitácora de todo el sistema: `docker compose logs | grep "bitacora"`.

## Alcance y decisiones

- **"Centralizado"** = todos los servicios escriben a stdout con el mismo formato y el mismo id, y Docker los reúne (`docker compose logs`). No se agrega un agregador (Loki, ELK, Dozzle): para el alcance del TP el correlation id resuelve el seguimiento entre servicios; un agregador sería el siguiente paso si hubiera que retener o consultar logs históricos.
- **Tareas programadas** (expiración de reservas, reintentos de emisión) no tienen un request de origen: sus líneas muestran el id vacío (`[msvc-ventas,]`).
- La identidad del MDC (`usuario`) se toma solo del header `X-User-Id` que llega a los servicios; en el gateway no se usa porque ahí el header todavía no está verificado.
