-- Migración para volúmenes ya creados (init.sql solo corre en DB vacía).
-- Email de destino resuelto al crear la notificación para el canal EMAIL (MailHog/SMTP).

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE notificacion ADD COLUMN destinatario VARCHAR(255) NULL',
    'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notificacion' AND COLUMN_NAME = 'destinatario');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
