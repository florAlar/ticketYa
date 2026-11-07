-- Migración opcional si el volumen MySQL de entradas ya existía sin columna precio.
-- No corre solo: init.sql solo en primer bootstrap del volume.
-- Ejemplo:
--   mysql -h 127.0.0.1 -P 3310 -u ticketya -p entradas < 02-add-entrada-precio.sql
-- O docker exec db-mysql-entradas mysql -uticketya -pticketya_dev entradas < ...

SET NAMES utf8mb4;

-- Idempotente: si la columna ya está, ignorar error 1060 en clientes que no soporten IF NOT EXISTS
ALTER TABLE entrada
  ADD COLUMN precio DECIMAL(12, 2) NOT NULL DEFAULT 0
    COMMENT 'precio unitario al emitir (reintegro/DTO)'
    AFTER id_sector;

-- Alinear seeds conocidos si siguen en 0
UPDATE entrada SET precio = 18000.00
 WHERE id_entrada = 'aa000000-0000-4000-8000-000000000001' AND precio = 0;
UPDATE entrada SET precio = 12000.00
 WHERE id_entrada = 'aa000000-0000-4000-8000-000000000002' AND precio = 0;
