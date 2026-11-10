-- Migración opcional si el volumen MySQL de eventos ya existía sin layout de sectores numerados.
-- No corre solo: init.sql solo en primer bootstrap del volume.
-- Asientos virtuales: el sector guarda filas x asientos_por_fila; no se persisten asientos vacíos.

SET NAMES utf8mb4;

ALTER TABLE sector
  ADD COLUMN filas INT NULL
    COMMENT 'solo sector numerado: cantidad de filas (A, B, ... AA)'
    AFTER es_numerado;

ALTER TABLE sector
  ADD COLUMN asientos_por_fila INT NULL
    COMMENT 'solo sector numerado: asientos por fila; capacidad = filas * asientos_por_fila'
    AFTER filas;

-- Sectores numerados existentes: 250 asientos por fila cuando la capacidad es múltiplo de 250.
UPDATE sector
SET filas = capacidad DIV 250,
    asientos_por_fila = 250
WHERE es_numerado = 1
  AND filas IS NULL
  AND capacidad > 0
  AND capacidad MOD 250 = 0;

-- Revisar a mano los numerados que queden sin layout (capacidad no múltiplo de 250):
-- SELECT id, nombre, capacidad FROM sector WHERE es_numerado = 1 AND filas IS NULL;
