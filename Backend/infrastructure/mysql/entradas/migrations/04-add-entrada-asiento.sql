-- Migración opcional si el volumen MySQL de entradas ya existía sin asiento virtual en entrada.
-- No corre solo: init.sql solo en primer bootstrap del volume.
-- Las entradas de sectores generales quedan con fila/numero NULL.

SET NAMES utf8mb4;

ALTER TABLE entrada
  ADD COLUMN fila VARCHAR(8) NULL
    COMMENT 'asiento virtual (solo sector numerado): fila'
    AFTER id_sector;

ALTER TABLE entrada
  ADD COLUMN numero INT NULL
    COMMENT 'asiento virtual (solo sector numerado): número en la fila'
    AFTER fila;
