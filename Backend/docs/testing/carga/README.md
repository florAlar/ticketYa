 Pruebas de carga TicketYa (JMeter)
 
Pruebas de carga contra el gateway local con dos planes:
 
| Plan | Flujo |
|---|---|
| `compra-normal.jmx` | Compra sin fila (evento no de alta demanda) |
| `compra-con-fila.jmx` | Compra con fila virtual: ingreso, poll de posición, validación, reserva, compra, pago y verificación |
 
## Requisitos
- Usuarios generados por `generar-usuarios.sh` (los JWT vencen a la hora).
- Stack levantado (gateway en `http://localhost:8080`):

```bash
cd Backend && docker compose up -d --build
```
- JMeter 5.x (probado con 5.6.3). El 2.13 de apt **no** sirve.
- `curl` y `jq` (los usa `generar-usuarios.sh`).
- `generar-usuarios.sh`, `jmeter.sh` y los `.jmx` en la misma carpeta.
## Uso rápido
 
Con el script completo (regenera el CSV y ejecuta el plan):
 
```bash
./jmeter.sh compra-con-fila.jmx 25    # compra con fila virtual, 25 usuarios
./jmeter.sh compra-normal.jmx 50      # compra sin fila, 50 usuarios
```
 
El reporte queda en `report/index.html`, dentro de esta carpeta.
 
## Uso manual
 
### 1. Generar el CSV de usuarios
 
El CSV tiene encabezado y columnas `email,token`. Se genera con cuentas nuevas en cada corrida:
 
```bash
CANT_USERS=25
CSV="$PWD/usuarios.csv"
OUT="$CSV" BASE="http://localhost:8080" \
  ./generar-usuarios.sh "$CANT_USERS" "$(( $(date +%s) % 1000000 ))"
```
 
El segundo argumento es un número base para que los emails no se repiten entre corridas.
 
### 2. Elegir plan, evento y sector
 
| Plan | `EVENTO` | `SECTOR` |
|---|---|---|
| `compra-con-fila.jmx` | `d0000000-0000-4000-8000-000000000001` | `c0000000-0000-4000-8000-000000000011` |
| `compra-normal.jmx` | `d0000000-0000-4000-8000-000000000006` | `c0000000-0000-4000-8000-000000000015` |
 
### 3. Ejecutar
 
```bash
rm -rf report corrida.jtl      # JMeter no pisa un reporte existente
 
jmeter -n -t "$PLAN" \
  -Jhost=localhost -Jport=8080 \
  -JidEvento="$EVENTO" -JidSector="$SECTOR" \
  -Jusuarios="$CANT_USERS" -Jramp="$(( CANT_USERS / 2 + 1 ))" \
  -Jiteraciones=1 -JloopForever=false \
  -Jcsv="$CSV" \
  -l corrida.jtl -e -o report
```
 
Abrí `report/index.html` al terminar.
 
## Parámetros (`-J<nombre>=valor`)
 
| Parámetro | Default | Descripción |
|---|---|---|
| `host` / `port` | `localhost` / `8080` | Destino. `host` va **sin** `http://` |
| `idEvento`, `idSector` | (vacío) | Obligatorios |
| `usuarios` | 25 | Usuarios virtuales (hilos) |
| `ramp` | 30 | Segundos en los que arrancan todos los usuarios. `0` = todos a la vez |
| `iteraciones` | -1 | Repeticiones por usuario. Usar `1` junto con `loopForever=false` |
| `loopForever` | true | Si es `true`, el usuario repite hasta cumplir `duracion` |
| `duracion` | 300 | Segundos máximos de la prueba |
| `csv` | `usuarios.csv` | Pool de usuarios. Usar ruta absoluta |
| `pausa` | 200 | Pausa (ms) entre iteraciones |
| `cantidad` | 1 | Entradas por reserva |
| `pollMax` | 30 | Máximo de consultas de posición en la fila |
| `pollInterval` | 2000 | Ms entre consultas de posición |
| `adminThreads` | 0 | Hilos admin (solo variante B2) |
| `adminToken` | (vacío) | JWT de un ADMIN (solo B2) |
| `adminPaso` | 25 | Posiciones que habilita el admin por tanda (B2) |
| `adminIntervalo` | 10000 | Ms entre tandas del admin (B2) |
 
## Variantes de la fila
 
- **B1 (default):** la tanda la habilita automáticamente el job del servicio.
- **B2 (habilitación manual):** definir `TICKETYA_FILA_AUTO_ENABLED=false` en `.env`/shell y recrear `ms-ventas` (`docker compose up -d --force-recreate ms-ventas`), y agregar:
```bash
-JadminThreads=1 -JadminToken=<JWT_ADMIN> -JadminPaso=25 -JadminIntervalo=10000
```
 
## Qué mirar en los resultados
 
- **Error %**: requests fallidos. En la fila, el paso `99` cuenta como error si el usuario no llegó a `HABILITADO` dentro de `pollMax` consultas.
- **Latencia**: media y percentiles (p95, p99). Los percentiles importan más que el promedio.
- **Throughput**: requests por segundo.
## Problemas frecuentes
 
| Síntoma | Causa probable |
|---|---|
| `El gateway no responde` | El stack no está levantado |
| Todos los requests fallan con 401 | Tokens del CSV vencidos o inválidos: regenerar el CSV |
| JMeter falla al generar el reporte | La carpeta `report/` ya existe: `jmeter.sh` la elimina al inicio; si corrés JMeter a mano, borrala |
| Errores de conexión con `-Jhost=http://...` | `host` no lleva protocolo |
| La corrida dura unos 5 minutos en vez de terminar sola | Agregar `-JloopForever=false` |
| `usuarios.csv` no se encuentra | Pasar ruta absoluta en `-Jcsv` |
 
