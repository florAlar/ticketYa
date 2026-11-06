-- msvc-eventos | DB: eventos
-- Schema + seed mínimo. id_organizador = ref lógica a usuarios.usuario (sin FK cross-DB).
-- IDs fijos: infrastructure/mysql/README.md

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS lugar (
  id               CHAR(36)     NOT NULL,
  nombre           VARCHAR(200) NOT NULL,
  direccion        VARCHAR(255) NULL,
  ciudad           VARCHAR(120) NOT NULL,
  capacidad_total  INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sector (
  id          CHAR(36)     NOT NULL,
  id_lugar    CHAR(36)     NOT NULL,
  nombre      VARCHAR(120) NOT NULL,
  capacidad   INT          NOT NULL DEFAULT 0,
  tipo        VARCHAR(64)  NULL,
  es_numerado TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_sector_lugar FOREIGN KEY (id_lugar) REFERENCES lugar (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asiento (
  id        CHAR(36)    NOT NULL,
  id_sector CHAR(36)    NOT NULL,
  fila      VARCHAR(16) NULL,
  numero    INT         NULL,
  codigo    VARCHAR(32) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_asiento_sector FOREIGN KEY (id_sector) REFERENCES sector (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evento (
  id                 CHAR(36)     NOT NULL,
  id_organizador     CHAR(36)     NOT NULL COMMENT 'ref lógica usuarios.usuario',
  nombre             VARCHAR(255) NOT NULL,
  descripcion        TEXT         NULL,
  tipo               VARCHAR(64)  NOT NULL,
  fecha_inicio       DATETIME(3)  NOT NULL,
  fecha_fin          DATETIME(3)  NULL,
  estado             VARCHAR(32)  NOT NULL,
  id_lugar           CHAR(36)     NOT NULL,
  fecha_publicacion  DATETIME(3)  NULL,
  es_alta_demanda    TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_evento_estado (estado),
  KEY ix_evento_tipo (tipo),
  KEY ix_evento_organizador (id_organizador),
  CONSTRAINT fk_evento_lugar FOREIGN KEY (id_lugar) REFERENCES lugar (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS precio_sector (
  id        CHAR(36)       NOT NULL,
  id_evento CHAR(36)       NOT NULL,
  id_sector CHAR(36)       NOT NULL,
  precio    DECIMAL(12, 2) NOT NULL,
  moneda    CHAR(3)        NOT NULL DEFAULT 'ARS',
  activo    TINYINT(1)     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_precio_evento_sector (id_evento, id_sector),
  CONSTRAINT fk_precio_sector_evento FOREIGN KEY (id_evento) REFERENCES evento (id),
  CONSTRAINT fk_precio_sector_sector FOREIGN KEY (id_sector) REFERENCES sector (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS precio_asiento (
  id         CHAR(36)       NOT NULL,
  id_evento  CHAR(36)       NOT NULL,
  id_asiento CHAR(36)       NOT NULL,
  precio     DECIMAL(12, 2) NOT NULL,
  moneda     CHAR(3)        NOT NULL DEFAULT 'ARS',
  activo     TINYINT(1)     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_precio_evento_asiento (id_evento, id_asiento),
  CONSTRAINT fk_precio_asiento_evento  FOREIGN KEY (id_evento)  REFERENCES evento (id),
  CONSTRAINT fk_precio_asiento_asiento FOREIGN KEY (id_asiento) REFERENCES asiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO lugar (id, nombre, direccion, ciudad, capacidad_total) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'Anfiteatro del Lago', 'Av. Costanera 100', 'Tandil', 620)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO sector (id, id_lugar, nombre, capacidad, tipo, es_numerado) VALUES
  ('c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000001', 'General', 500, 'GENERAL', 0),
  ('c0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000001', 'Platea',  120, 'PLATEA',  1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO asiento (id, id_sector, fila, numero, codigo) VALUES
  ('c0000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000012', 'A', 1, 'A-1'),
  ('c0000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000012', 'A', 2, 'A-2')
ON DUPLICATE KEY UPDATE codigo = VALUES(codigo);

INSERT INTO evento (id, id_organizador, nombre, descripcion, tipo, fecha_inicio, fecha_fin, estado, id_lugar, fecha_publicacion, es_alta_demanda) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
   'Noche de Indie en el Lago', 'Recital seed demo', 'RECITAL',
   '2026-11-15 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-01 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002',
   'Hamlet — función especial', 'Teatro seed demo', 'TEATRO',
   '2026-11-20 20:30:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-01 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000002',
   'Festival cancelado (demo)', 'Evento cancelado para cola admin', 'FESTIVAL',
   '2026-12-01 20:00:00.000', NULL, 'CANCELADO',
   'c0000000-0000-4000-8000-000000000001', '2026-09-01 12:00:00.000', 0)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO precio_sector (id, id_evento, id_sector, precio, moneda, activo) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011', 18000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000012', 25000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000011', 10000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000012', 12000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000011', 15000.00, 'ARS', 0)
ON DUPLICATE KEY UPDATE precio = VALUES(precio);

INSERT INTO precio_asiento (id, id_evento, id_asiento, precio, moneda, activo) VALUES
  ('e0000000-0000-4000-8000-000000000011', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000021', 12000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000012', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000022', 12000.00, 'ARS', 1)
ON DUPLICATE KEY UPDATE precio = VALUES(precio);
