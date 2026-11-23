# Especificaciones de trabajo — flujo Spec-Driven

> **Vigente desde 2026-11-22.** La consigna lo pide en `consigna.md` §7: escribir y revisar la
> especificación **antes** de implementar, versionarla con el código y usarla como base de los
> contratos OpenAPI y de los casos de prueba. Esta carpeta es la fuente de verdad del equipo.

## 1. Principio

- Toda feature (US, bug relevante, cambio de contrato o prueba no funcional) se **escribe y revisa en
  `specs/`** antes de tocar código.
- La spec se versiona en Git junto al código; no vive en un documento aparte ni solo en Trello.
- La spec guía el **contrato** (`endpoints.md` + OpenAPI), los **tests** y la **documentación formal**
  (`docs/`). Si el código y la spec difieren, se corrige la spec o el código en el mismo PR.
- Los agentes de IA deben leer `AGENTS.md`, `consigna.md`, `backlog.md`, `reglas-de-negocio.md` y la
  spec de la feature **antes** de proponer o escribir código.
- No se inventan reglas: ante ambigüedad se consulta o se deja una decisión explícita en la spec.

## 2. Ciclo de trabajo

| Paso | Qué se produce | Dónde | Criterio para avanzar |
| --- | --- | --- | --- |
| 1. Propuesta | ID/US, problema, objetivo, alcance y fuera de alcance | `backlog.md` (fila) + spec de la feature | El equipo acuerda qué entra y qué no |
| 2. Requisitos | Escenarios, criterios de aceptación, roles, casos borde y errores | Spec §Requisitos / CA | CA verificables (status/estado esperado) |
| 3. Diseño | API (paths/DTO/roles/códigos), eventos, modelo, dependencias, decisiones y alternativas | Spec §Diseño + `endpoints.md`/`comunicacion.md` si aplica | Contrato revisado; OpenAPI y Postman planificados |
| 4. Tareas | Commits/US, archivos, tests, docs a tocar y orden | Spec §Tareas o plan de commits | Tareas chicas, cada una verificable |
| 5. Implementación y cierre | Código + tests + docs + backlog en el mismo commit/PR; evidencia | Repo + `backlog.md` + `docs/` | CA verdes, evidencia adjunta, backlog actualizado |

El orden de la consigna es **Propuesta → Requisitos y escenarios → Diseño → Tareas → Implementación**.
La Parte A de una feature puede adelantarse si desbloquea un entregable obligatorio (por ejemplo,
`TST-CARGA-01/A` antes de la fila virtual), pero la spec debe reflejarlo.

## 3. Dónde vive cada cosa

| Artefacto | Archivo | Rol |
| --- | --- | --- |
| Alcance y entregables | `consigna.md` | No se modifica; manda sobre todo |
| Estado y prioridad | `backlog.md` | Una fila por US; `hecho`/`parcial`/`pendiente` |
| Reglas transversales | `reglas-de-negocio.md` | Decisiones de negocio y puntos abiertos |
| Política de roles | `politica-de-roles.md` | Aprovisionamiento, permisos y matriz (X-ORG-02) |
| Contrato HTTP | `endpoints.md` + `docs/openapi/` + `<msvc>/docs/endpoints.md` | Paths, roles, códigos, DTOs |
| Comunicación | `comunicacion.md` | Gateway, Feign, Rabbit, idempotencia y fallos |
| Mapa de servicios | `microservicios.md` | Responsabilidades, APIs y límites |
| Mapa del repo | `estructura.md` | Árbol real de carpetas, capas y dónde vive cada artefacto |
| Bases de datos | `bases-de-datos.md` + `diagramas/modelo*.md` + `../docs/modelo-datos.md` | Motores e índices al modelo de datos |
| Diagramas | `diagramas/` | ER MySQL/Mongo y secuencias de compra/devolución |
| Spec de feature | `fila-virtual.md`, `integracion-entre-msvc.md`, `carga-e2-tst-03.md`, … | Decisiones, diseño, CA y plan |
| Registro histórico | `documentacion-y-validacion.md` | Línea E1 “Documentación y Validación” (cerrada; se conserva como registro) |
| Contexto por servicio | `plantilla-docs-msvc.md` + `<msvc>/docs/README.md` | DOC-MS-00 |
| Documentación de entrega | `../docs/` | Arquitectura, modelo, observabilidad, testing, OpenAPI, Postman |

Regla: **enlazar, no duplicar**. La spec de feature puede resumir el contrato, pero el detalle vive en
`endpoints.md`/OpenAPI; el contexto de un servicio vive en su `docs/README.md`.

## 4. Convenciones

- **Nombre:** `specs/<tema-en-kebab>.md` (por ejemplo `fila-virtual.md`, `carga-e2-tst-03.md`).
- **Encabezado:** ID, estado, fecha, alcance, “no incluye” y dueño. Actualizar la fecha al cambiar.
- **Estados:** `propuesta` → `parcial` (implementación en curso) → `hecho`. No usar `hecho` con CA
  abiertos o evidencia faltante.
- **CA:** redactados como “dado/cuando/entonces” con status o estado observable y el test que lo cubre.
- **Trazabilidad:** cada CA debe poder rastrearse a un test (unitario/IT/E2E/Postman/JMeter) y a la
  documentación afectada.
- **Decisiones:** registrar la elegida y las alternativas descartadas; si una decisión se revierte,
  dejar la nota en la spec (no borrar el historial).
- **Commits:** un commit por paso verificable, convencional (`feat`, `fix`, `test`, `docs`, `refactor`)
  y con el ID de la US en el scope (`feat(fila): …`).
- **No trabajar en `main`:** rama nueva desde `origin/main`, PR y merge.

## 5. Definición de terminado (DoD)

Una feature está terminada cuando:

- [ ] La spec existe, está revisada y su estado/fecha están al día.
- [ ] El contrato está en `endpoints.md`, OpenAPI y Postman (si aplica).
- [ ] Los tests cubren los CA (unitarios + IT/E2E/carga según el riesgo).
- [ ] La documentación formal (`docs/` y `<msvc>/docs/`) está actualizada.
- [ ] `backlog.md` tiene la fila en `hecho` con fecha, resumen y evidencia.
- [ ] El PR linkea la spec, los resultados y los comandos reproducibles.
- [ ] `git diff --check` y el árbol de trabajo quedan limpios.

## 6. Checklists

**Antes de empezar**

- [ ] Leí `consigna.md`, `backlog.md`, `reglas-de-negocio.md` y la spec de la feature.
- [ ] La spec tiene alcance, CA y decisiones; si no, la escribo/actualizo primero.
- [ ] Identifiqué impacto en gateway, contratos, OpenAPI, Postman, front y tests.
- [ ] Creé la rama desde `origin/main`.

**Durante**

- [ ] Respeto las capas del módulo (`controller/service/repository/entity|document/dto/mapper/clients/exception/config`).
- [ ] Tests junto al código; no dejo `@Disabled` sin justificar.
- [ ] Actualizo `endpoints.md`/OpenAPI/Postman en el mismo PR si cambia el contrato.
- [ ] Registro decisiones y desvíos en la spec.

**Al cerrar**

- [ ] Corrí los tests del módulo y el build/lint del front si aplica.
- [ ] Actualicé `backlog.md` y la documentación afectada.
- [ ] La evidencia está en el PR (comandos, resultados, capturas o reportes).
- [ ] No commiteé `.env`, credenciales ni artefactos generados.

## 7. Ejemplos para copiar

- Feature de negocio con decisiones y carga: [`fila-virtual.md`](fila-virtual.md).
- Pruebas de integración entre servicios: [`integracion-entre-msvc.md`](integracion-entre-msvc.md).
- Plan y resultados de carga: [`carga-e2-tst-03.md`](carga-e2-tst-03.md).
- Contexto de un microservicio: [`plantilla-docs-msvc.md`](plantilla-docs-msvc.md).

## 8. Excepciones

- Un **hotfix trivial** (typo, lint, dependencia) puede ir directo a PR; si cambia el estado de una US,
  se actualiza el backlog igual.
- Un **cambio de alcance** se escribe en la spec **antes** de implementarlo; no se “descubre” en el
  código.
- Si no hay tiempo para una spec completa, se registra al menos: problema, CA, contrato y tareas en
  `backlog.md` + una spec corta; nunca se implementa a ciegas.

## 9. Sobre el directorio `openspec/` (eliminado)

El repositorio tuvo un `openspec/` en la raíz con cuatro specs en Given/When/Then generadas al inicio
(2026-11-09). **Se eliminó el 2026-11-22 porque no tiene sentido mantenerlo:** duplicaba las
especificaciones y quedó desactualizado desde 2026-11-10 (por ejemplo, seguía describiendo la venta
solo por cupo de sector, sin asientos virtuales), así que cada cambio obligaba a sincronizar dos
fuentes en lugar de una.

La fuente de verdad es esta carpeta (`Backend/specs/`). Los criterios de aceptación se siguen
redactando como dado/cuando/entonces (§4) y la consigna se cumple igual: specs versionadas junto al
código y base de los contratos OpenAPI y de los tests. No hace falta reinstalar OpenSpec ni recrear
ese directorio.
