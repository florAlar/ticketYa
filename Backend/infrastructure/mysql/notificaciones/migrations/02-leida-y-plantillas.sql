-- Migración para volúmenes ya creados (init.sql solo corre en DB vacía).
-- 1) "Leída" en la bandeja de la app.
-- 2) Plantillas de los eventos nuevos (suspensión, modificación, favorito, compra).

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD COLUMN leida TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND COLUMN_NAME = 'leida');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD COLUMN fecha_lectura DATETIME(3) NULL',
    'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND COLUMN_NAME = 'fecha_lectura');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD COLUMN clave_idempotencia VARCHAR(128) NULL',
    'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND COLUMN_NAME = 'clave_idempotencia');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD UNIQUE KEY uk_notif_usuario_clave (id_usuario, clave_idempotencia)',
    'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND INDEX_NAME = 'uk_notif_usuario_clave');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD KEY ix_notif_usuario_fecha (id_usuario, fecha_creacion)',
    'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND INDEX_NAME = 'ix_notif_usuario_fecha');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD KEY ix_notif_estado_fecha (estado, fecha_creacion)',
    'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND INDEX_NAME = 'ix_notif_estado_fecha');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
   '¡{{evento}} en {{ciudad}}! Un artista que seguís se presenta el {{fecha}}.', 1)
ON DUPLICATE KEY UPDATE
  tipo_evento = VALUES(tipo_evento), canal = VALUES(canal), asunto = VALUES(asunto),
  contenido = VALUES(contenido), activa = VALUES(activa);
