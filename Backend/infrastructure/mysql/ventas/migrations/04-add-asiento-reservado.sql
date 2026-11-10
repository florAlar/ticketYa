-- Migración opcional si el volumen MySQL de ventas ya existía sin asientos virtuales.
-- No corre solo: init.sql solo en primer bootstrap del volume.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS asiento_reservado (
  id_asiento_reservado CHAR(36)    NOT NULL,
  id_reserva           CHAR(36)    NOT NULL,
  id_evento            CHAR(36)    NOT NULL COMMENT 'ref lógica eventos.evento',
  id_sector            CHAR(36)    NOT NULL COMMENT 'ref lógica eventos.sector',
  fila                 VARCHAR(8)  NOT NULL,
  numero               INT         NOT NULL,
  estado               VARCHAR(16) NOT NULL COMMENT 'RESERVADO|VENDIDO',
  PRIMARY KEY (id_asiento_reservado),
  UNIQUE KEY uk_asiento_evento_sector_fila_numero (id_evento, id_sector, fila, numero),
  KEY ix_asiento_reserva (id_reserva),
  CONSTRAINT fk_asiento_reserva FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
