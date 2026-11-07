-- Migración opcional si el volumen MySQL de eventos ya existía sin participantes.
-- No corre solo: init.sql solo en primer bootstrap del volume.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS participante (
  id          CHAR(36)     NOT NULL,
  nombre      VARCHAR(200) NOT NULL,
  tipo        VARCHAR(64)  NULL COMMENT 'ARTISTA|BANDA|…',
  descripcion TEXT         NULL,
  PRIMARY KEY (id),
  KEY ix_participante_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evento_participante (
  id_evento       CHAR(36)    NOT NULL,
  id_participante CHAR(36)    NOT NULL,
  rol             VARCHAR(64) NULL COMMENT 'PRINCIPAL|TELONERO|…',
  orden           INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id_evento, id_participante),
  KEY ix_ep_participante (id_participante),
  CONSTRAINT fk_ep_evento FOREIGN KEY (id_evento) REFERENCES evento (id) ON DELETE CASCADE,
  CONSTRAINT fk_ep_participante FOREIGN KEY (id_participante) REFERENCES participante (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO participante (id, nombre, tipo, descripcion) VALUES
  ('p0000000-0000-4000-8000-000000000001', 'Metallica', 'BANDA', 'Thrash metal — M72 World Tour'),
  ('p0000000-0000-4000-8000-000000000002', 'Gojira', 'BANDA', 'Soporte M72'),
  ('p0000000-0000-4000-8000-000000000003', 'Real Madrid CF', 'EQUIPO', 'Finalista Champions League'),
  ('p0000000-0000-4000-8000-000000000004', 'Manchester City FC', 'EQUIPO', 'Finalista Champions League'),
  ('p0000000-0000-4000-8000-000000000005', 'Iron Maiden', 'BANDA', 'Headliner Rock in Rio (cancelado)'),
  ('p0000000-0000-4000-8000-000000000006', 'Ghost', 'BANDA', 'Soporte Rock in Rio (cancelado)')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), tipo = VALUES(tipo), descripcion = VALUES(descripcion);

INSERT INTO evento_participante (id_evento, id_participante, rol, orden) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'p0000000-0000-4000-8000-000000000001', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000001', 'p0000000-0000-4000-8000-000000000002', 'TELONERO', 1),
  ('d0000000-0000-4000-8000-000000000002', 'p0000000-0000-4000-8000-000000000003', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000002', 'p0000000-0000-4000-8000-000000000004', 'PRINCIPAL', 1),
  ('d0000000-0000-4000-8000-000000000003', 'p0000000-0000-4000-8000-000000000005', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000003', 'p0000000-0000-4000-8000-000000000006', 'TELONERO', 1)
ON DUPLICATE KEY UPDATE rol = VALUES(rol), orden = VALUES(orden);
