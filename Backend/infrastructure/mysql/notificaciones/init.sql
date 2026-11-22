-- msvc-notificaciones | DB: notificaciones
-- Schema + seed mínimo (plantilla). id_usuario / id_evento_origen = refs lógicas.
-- IDs fijos: infrastructure/mysql/README.md

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS plantilla_notificacion (
  id_plantilla CHAR(36)     NOT NULL,
  tipo_evento  VARCHAR(64)  NOT NULL,
  canal        VARCHAR(32)  NOT NULL,
  asunto       VARCHAR(255) NOT NULL,
  contenido    TEXT         NOT NULL,
  activa       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notificacion (
  id_notificacion  CHAR(36)     NOT NULL,
  id_usuario       CHAR(36)     NOT NULL COMMENT 'ref lógica usuarios.usuario',
  id_evento_origen CHAR(36)     NULL COMMENT 'ref lógica eventos.evento',
  id_plantilla     CHAR(36)     NULL,
  tipo             VARCHAR(64)  NOT NULL,
  canal            VARCHAR(32)  NOT NULL,
  asunto           VARCHAR(255) NOT NULL,
  contenido        TEXT         NOT NULL,
  destinatario     VARCHAR(255) NULL COMMENT 'email resuelto para canal EMAIL',
  fecha_creacion   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  fecha_envio      DATETIME(3)  NULL,
  estado           VARCHAR(32)  NOT NULL,
  leida            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'leída por el usuario en la app',
  fecha_lectura    DATETIME(3)  NULL,
  clave_idempotencia VARCHAR(128) NULL COMMENT 'tipo:canal:eventoId:messageId | CompraConfirmada:EMAIL:compraId',
  clave_mensaje      VARCHAR(128) NULL COMMENT 'identidad del mensaje sin canal (PUSH/EMAIL comparten clave)',
  PRIMARY KEY (id_notificacion),
  KEY ix_notif_usuario (id_usuario),
  KEY ix_notif_usuario_fecha (id_usuario, fecha_creacion),
  KEY ix_notif_usuario_mensaje (id_usuario, clave_mensaje),
  KEY ix_notif_estado_fecha (estado, fecha_creacion),
  UNIQUE KEY uk_notif_usuario_clave (id_usuario, clave_idempotencia),
  CONSTRAINT fk_notif_plantilla FOREIGN KEY (id_plantilla) REFERENCES plantilla_notificacion (id_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS intento_envio (
  id_intento       CHAR(36)     NOT NULL,
  id_notificacion  CHAR(36)     NOT NULL,
  numero_intento   INT          NOT NULL DEFAULT 1,
  fecha_intento    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  resultado        VARCHAR(32)  NOT NULL,
  detalle_error    VARCHAR(500) NULL,
  PRIMARY KEY (id_intento),
  CONSTRAINT fk_intento_notif FOREIGN KEY (id_notificacion) REFERENCES notificacion (id_notificacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO plantilla_notificacion (id_plantilla, tipo_evento, canal, asunto, contenido, activa) VALUES
  ('n0000000-0000-4000-8000-000000000001', 'CompraConfirmada', 'EMAIL',
   'Tu compra en TicketYa',
   'Hola, tu compra {{compraId}} por {{cantidadEntradas}} entrada(s) fue confirmada. Total: {{total}} {{moneda}}. Evento: {{eventoId}}.', 1),
  ('n0000000-0000-4000-8000-000000000002', 'EventoCancelado', 'PUSH',
   'Evento cancelado',
   'El evento {{evento}} del {{fecha}} fue cancelado. Gestionaremos el reintegro.', 1),
  ('n0000000-0000-4000-8000-000000000003', 'EventoSuspendido', 'PUSH',
   'Evento suspendido',
   'El evento {{evento}} del {{fecha}} fue suspendido. Te avisaremos cuando se reprograme.', 1),
  ('n0000000-0000-4000-8000-000000000004', 'EventoModificado', 'PUSH',
   'Cambió un evento que te interesa',
   'El evento {{evento}} cambió: {{cambios}}. Nueva fecha: {{fecha}}.', 1),
  ('n0000000-0000-4000-8000-000000000005', 'NuevoEventoArtistaFavorito', 'PUSH',
   'Tu artista favorito se presenta',
   '¡{{evento}} en {{ciudad}}! Un artista que seguís se presenta el {{fecha}}.', 1),
  ('n0000000-0000-4000-8000-000000000006', 'EventoCancelado', 'EMAIL',
   'Evento cancelado',
   'El evento {{evento}} del {{fecha}} fue cancelado. Gestionaremos el reintegro.', 1),
  ('n0000000-0000-4000-8000-000000000007', 'EventoSuspendido', 'EMAIL',
   'Evento suspendido',
   'El evento {{evento}} del {{fecha}} fue suspendido. Te avisaremos cuando se reprograme.', 1),
  ('n0000000-0000-4000-8000-000000000008', 'EventoModificado', 'EMAIL',
   'Cambió un evento que te interesa',
   'El evento {{evento}} cambió: {{cambios}}. Nueva fecha: {{fecha}}.', 1),
  ('n0000000-0000-4000-8000-000000000009', 'NuevoEventoArtistaFavorito', 'EMAIL',
   'Tu artista favorito se presenta',
   '¡{{evento}} en {{ciudad}}! Un artista que seguís se presenta el {{fecha}}.', 1),
  ('n0000000-0000-4000-8000-00000000000a', 'DevolucionAprobada', 'PUSH',
   'Devolución aprobada',
   'Aprobamos la devolución de tu entrada para el evento {{eventoId}}. Reintegro: {{monto}} {{moneda}}.', 1),
  ('n0000000-0000-4000-8000-00000000000b', 'DevolucionAprobada', 'EMAIL',
   'Devolución aprobada',
   'Aprobamos la devolución de tu entrada para el evento {{eventoId}}. Reintegro: {{monto}} {{moneda}}.', 1)
ON DUPLICATE KEY UPDATE
  tipo_evento = VALUES(tipo_evento), canal = VALUES(canal), asunto = VALUES(asunto),
  contenido = VALUES(contenido), activa = VALUES(activa);

-- Sin notificaciones/intentos seed (solo catálogo de plantillas)


-- ============================================================================
-- Plataforma en funcionamiento: bandeja de notificaciones con leídas, sin leer, pendientes y fallidas.
-- ============================================================================
INSERT INTO notificacion (id_notificacion, id_usuario, id_evento_origen, id_plantilla, tipo, canal, asunto, contenido, destinatario, fecha_creacion, fecha_envio, estado, leida, fecha_lectura, clave_idempotencia, clave_mensaje) VALUES
  ('n0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000021', 'n0000000-0000-4000-8000-000000000001', 'CompraConfirmada', 'EMAIL',
   'Tu compra en TicketYa', 'Hola, tu compra por 1 entrada(s) fue confirmada. Total: 60000 ARS. Evento: Supercross Indoor — Gran Final.',
   'demo@ticketya.com', '2026-11-10 18:25:00.000', '2026-11-10 18:25:05.000', 'ENVIADA', 1, '2026-11-10 19:25:00.000', 'CompraConfirmada:EMAIL:d0000000-0000-4000-8000-000000000021:seed-1', 'CompraConfirmada:d0000000-0000-4000-8000-000000000021:seed-1'),
  ('n0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000016', 'n0000000-0000-4000-8000-000000000001', 'CompraConfirmada', 'EMAIL',
   'Tu compra en TicketYa', 'Hola, tu compra por 2 entrada(s) fue confirmada. Total: 260000 ARS. Evento: Sepultura — Roots Bloody Roots Night.',
   'demo@ticketya.com', '2026-11-12 11:10:00.000', '2026-11-12 11:10:05.000', 'ENVIADA', 1, '2026-11-12 12:10:00.000', 'CompraConfirmada:EMAIL:d0000000-0000-4000-8000-000000000016:seed-2', 'CompraConfirmada:d0000000-0000-4000-8000-000000000016:seed-2'),
  ('n0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000003', 'n0000000-0000-4000-8000-000000000002', 'EventoCancelado', 'PUSH',
   'Evento cancelado', 'El evento Rock in Rio — día metal (cancelado) fue cancelado. Gestionaremos el reintegro.',
   NULL, '2026-11-12 09:00:00.000', '2026-11-12 09:00:05.000', 'ENVIADA', 0, NULL, 'EventoCancelado:PUSH:d0000000-0000-4000-8000-000000000003:seed-3', 'EventoCancelado:d0000000-0000-4000-8000-000000000003:seed-3'),
  ('n0000000-0000-4000-8000-000000000104', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000002', 'n0000000-0000-4000-8000-000000000003', 'EventoSuspendido', 'PUSH',
   'Evento suspendido', 'El evento UEFA Champions League Final 2027 fue suspendido. Te avisaremos cuando se reprograme.',
   NULL, '2026-11-13 10:30:00.000', '2026-11-13 10:30:05.000', 'ENVIADA', 0, NULL, 'EventoSuspendido:PUSH:d0000000-0000-4000-8000-000000000002:seed-4', 'EventoSuspendido:d0000000-0000-4000-8000-000000000002:seed-4'),
  ('n0000000-0000-4000-8000-000000000105', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000017', 'n0000000-0000-4000-8000-000000000005', 'NuevoEventoArtistaFavorito', 'PUSH',
   'Tu artista favorito se presenta', '¡Mastodon — Hushed and Grim Live en Buenos Aires! Un artista que seguís (Gojira) se presenta el 5 de diciembre.',
   NULL, '2026-10-28 12:05:00.000', '2026-10-28 12:05:05.000', 'ENVIADA', 0, NULL, 'NuevoEventoArtistaFavorito:PUSH:d0000000-0000-4000-8000-000000000017:seed-5', 'NuevoEventoArtistaFavorito:d0000000-0000-4000-8000-000000000017:seed-5'),
  ('n0000000-0000-4000-8000-000000000106', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000012', 'n0000000-0000-4000-8000-00000000000a', 'DevolucionAprobada', 'PUSH',
   'Devolución aprobada', 'Aprobamos la devolución de tu entrada para Boca Juniors vs River Plate — Superclásico. Reintegro: 140000 ARS.',
   NULL, '2026-11-17 15:10:00.000', '2026-11-17 15:10:05.000', 'ENVIADA', 1, '2026-11-17 16:10:00.000', 'DevolucionAprobada:PUSH:d0000000-0000-4000-8000-000000000012:seed-6', 'DevolucionAprobada:d0000000-0000-4000-8000-000000000012:seed-6'),
  ('n0000000-0000-4000-8000-000000000107', 'b0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000018', 'n0000000-0000-4000-8000-000000000008', 'EventoModificado', 'EMAIL',
   'Cambió un evento que te interesa', 'El evento Judas Priest — Invincible Shield Tour cambió: horario de apertura de puertas.',
   'demo@ticketya.com', '2026-11-21 08:00:00.000', NULL, 'PENDIENTE', 0, NULL, 'EventoModificado:EMAIL:d0000000-0000-4000-8000-000000000018:seed-7', 'EventoModificado:d0000000-0000-4000-8000-000000000018:seed-7'),
  ('n0000000-0000-4000-8000-000000000108', 'b0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005', 'n0000000-0000-4000-8000-000000000001', 'CompraConfirmada', 'EMAIL',
   'Tu compra en TicketYa', 'Hola, tu compra por 2 entrada(s) fue confirmada. Total: 220000 ARS. Evento: Rammstein — Stadium Tour.',
   'lucia@ticketya.com', '2026-11-19 22:15:00.000', '2026-11-19 22:15:05.000', 'ENVIADA', 1, '2026-11-19 23:15:00.000', 'CompraConfirmada:EMAIL:d0000000-0000-4000-8000-000000000005:seed-8', 'CompraConfirmada:d0000000-0000-4000-8000-000000000005:seed-8'),
  ('n0000000-0000-4000-8000-000000000109', 'b0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-00000000001e', 'n0000000-0000-4000-8000-000000000003', 'EventoSuspendido', 'PUSH',
   'Evento suspendido', 'El evento WRC — Rally de Argentina fue suspendido. Te avisaremos cuando se reprograme.',
   NULL, '2026-11-16 09:00:00.000', '2026-11-16 09:00:05.000', 'ENVIADA', 0, NULL, 'EventoSuspendido:PUSH:d0000000-0000-4000-8000-00000000001e:seed-9', 'EventoSuspendido:d0000000-0000-4000-8000-00000000001e:seed-9'),
  ('n0000000-0000-4000-8000-00000000010a', 'b0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000003', 'n0000000-0000-4000-8000-000000000002', 'EventoCancelado', 'PUSH',
   'Evento cancelado', 'El evento Rock in Rio — día metal (cancelado) fue cancelado. Gestionaremos el reintegro.',
   NULL, '2026-11-12 09:00:00.000', '2026-11-12 09:00:05.000', 'ENVIADA', 0, NULL, 'EventoCancelado:PUSH:d0000000-0000-4000-8000-000000000003:seed-10', 'EventoCancelado:d0000000-0000-4000-8000-000000000003:seed-10'),
  ('n0000000-0000-4000-8000-00000000010b', 'b0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000002', 'n0000000-0000-4000-8000-000000000003', 'EventoSuspendido', 'PUSH',
   'Evento suspendido', 'El evento UEFA Champions League Final 2027 fue suspendido. Te avisaremos cuando se reprograme.',
   NULL, '2026-11-13 10:30:00.000', '2026-11-13 10:30:05.000', 'ENVIADA', 1, '2026-11-13 11:30:00.000', 'EventoSuspendido:PUSH:d0000000-0000-4000-8000-000000000002:seed-11', 'EventoSuspendido:d0000000-0000-4000-8000-000000000002:seed-11'),
  ('n0000000-0000-4000-8000-00000000010c', 'b0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000017', 'n0000000-0000-4000-8000-000000000001', 'CompraConfirmada', 'EMAIL',
   'Tu compra en TicketYa', 'Hola, tu compra por 1 entrada(s) fue confirmada. Total: 78000 ARS. Evento: Mastodon — Hushed and Grim Live.',
   'camila@ticketya.com', '2026-11-21 09:55:00.000', '2026-11-21 09:55:05.000', 'FALLIDA', 0, NULL, 'CompraConfirmada:EMAIL:d0000000-0000-4000-8000-000000000017:seed-12', 'CompraConfirmada:d0000000-0000-4000-8000-000000000017:seed-12')
ON DUPLICATE KEY UPDATE estado = VALUES(estado), leida = VALUES(leida);
