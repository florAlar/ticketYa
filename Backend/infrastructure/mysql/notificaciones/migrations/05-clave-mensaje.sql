-- Migración para volúmenes ya creados (init.sql solo corre en DB vacía).
-- La bandeja muestra una sola entrada por mensaje: las filas PUSH y EMAIL de un mismo aviso
-- comparten clave_mensaje. La clave de idempotencia de eventos ahora incluye el messageId de
-- RabbitMQ para que dos modificaciones distintas del mismo evento no se colapsen.

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD COLUMN clave_mensaje VARCHAR(128) NULL',
    'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND COLUMN_NAME = 'clave_mensaje');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD KEY ix_notif_usuario_mensaje (id_usuario, clave_mensaje)',
    'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND INDEX_NAME = 'ix_notif_usuario_mensaje');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill de filas previas quitando el segundo segmento (canal):
--   tipo:canal:origen            → tipo:origen
--   tipo:canal:origen:messageId  → tipo:origen:messageId
-- Sirve tanto para el formato viejo como si la migración corre después de crear filas nuevas.
UPDATE notificacion
SET clave_mensaje = CONCAT(
    SUBSTRING_INDEX(clave_idempotencia, ':', 1),
    SUBSTRING(
        clave_idempotencia,
        LENGTH(SUBSTRING_INDEX(clave_idempotencia, ':', 2)) + 1))
WHERE clave_idempotencia IS NOT NULL
  AND clave_mensaje IS NULL;

-- Notificaciones administrativas: clave única para que no se agrupen entre sí.
UPDATE notificacion
SET clave_mensaje = CONCAT('ADMIN:', id_notificacion)
WHERE clave_idempotencia IS NULL
  AND clave_mensaje IS NULL;
