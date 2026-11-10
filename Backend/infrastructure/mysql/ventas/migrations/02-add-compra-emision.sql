-- Migración opcional si el volumen MySQL de ventas ya existía sin columnas de emisión QR.
-- No corre solo: init.sql solo en primer bootstrap del volume.
-- Ejemplo:
--   mysql -h 127.0.0.1 -P 3309 -u ticketya -p ventas < 02-add-compra-emision.sql
-- O docker exec -i db-mysql-ventas mysql -uticketya -pticketya_dev ventas < ...

SET NAMES utf8mb4;

ALTER TABLE compra
  ADD COLUMN emision_estado VARCHAR(16) NOT NULL DEFAULT 'NO_REQUIERE'
    COMMENT 'NO_REQUIERE|PENDIENTE|EMITIDA|FALLIDA — QR msvc-entradas'
    AFTER estado;

ALTER TABLE compra
  ADD COLUMN emision_intentos INT NOT NULL DEFAULT 0
    AFTER emision_estado;

ALTER TABLE compra
  ADD COLUMN emision_ultimo_error VARCHAR(255) NULL
    AFTER emision_intentos;

ALTER TABLE compra
  ADD COLUMN emision_actualizado DATETIME(3) NULL
    AFTER emision_ultimo_error;

-- Compras ya aprobadas (seed u operativas) se asumen emitidas
UPDATE compra
SET emision_estado = 'EMITIDA',
    emision_intentos = GREATEST(COALESCE(emision_intentos, 0), 1),
    emision_ultimo_error = NULL,
    emision_actualizado = UTC_TIMESTAMP(3)
WHERE estado = 'APROBADA';
