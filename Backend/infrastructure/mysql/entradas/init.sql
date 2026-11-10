-- msvc-entradas | DB: entradas
-- Schema + seed mínimo. id_compra/cliente/evento/sector = refs lógicas.
-- IDs fijos: infrastructure/mysql/README.md

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS entrada (
  id_entrada        CHAR(36)       NOT NULL,
  id_compra         CHAR(36)       NOT NULL COMMENT 'ref lógica ventas.compra',
  id_cliente        CHAR(36)       NOT NULL COMMENT 'ref lógica usuarios.usuario',
  id_evento         CHAR(36)       NOT NULL COMMENT 'ref lógica eventos.evento',
  id_sector         CHAR(36)       NOT NULL COMMENT 'ref lógica eventos.sector',
  fila              VARCHAR(8)     NULL COMMENT 'asiento virtual (solo sector numerado): fila',
  numero            INT            NULL COMMENT 'asiento virtual (solo sector numerado): número en la fila',
  precio            DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT 'precio unitario al emitir (reintegro/DTO)',
  codigo_qr         VARCHAR(128)   NOT NULL,
  estado            VARCHAR(32)    NOT NULL,
  fecha_emision     DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  fecha_validacion  DATETIME(3)    NULL,
  PRIMARY KEY (id_entrada),
  UNIQUE KEY uk_entrada_qr (codigo_qr),
  KEY ix_entrada_cliente (id_cliente),
  KEY ix_entrada_evento (id_evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS devolucion (
  id_devolucion    CHAR(36)       NOT NULL,
  id_entrada       CHAR(36)       NOT NULL,
  motivo           VARCHAR(500)   NULL,
  monto            DECIMAL(12, 2) NOT NULL DEFAULT 0,
  fecha_solicitud  DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  estado           VARCHAR(32)    NOT NULL,
  PRIMARY KEY (id_devolucion),
  CONSTRAINT fk_devolucion_entrada FOREIGN KEY (id_entrada) REFERENCES entrada (id_entrada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS validacion_acceso (
  id_validacion       CHAR(36)     NOT NULL,
  id_entrada          CHAR(36)     NOT NULL,
  id_personal_acceso  CHAR(36)     NOT NULL COMMENT 'ref lógica usuarios.usuario PERSONAL_ACCESO',
  fecha_validacion    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  resultado           VARCHAR(32)  NOT NULL,
  dispositivo         VARCHAR(128) NULL,
  PRIMARY KEY (id_validacion),
  CONSTRAINT fk_validacion_entrada FOREIGN KEY (id_entrada) REFERENCES entrada (id_entrada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entrada válida (Metallica) + seed cancelado (Rock in Rio)
INSERT INTO entrada (id_entrada, id_compra, id_cliente, id_evento, id_sector, precio, codigo_qr, estado, fecha_emision, fecha_validacion) VALUES
  ('aa000000-0000-4000-8000-000000000001',
   'f0000000-0000-4000-8000-000000000021',
   'b0000000-0000-4000-8000-000000000001',
   'd0000000-0000-4000-8000-000000000001',
   'c0000000-0000-4000-8000-000000000011',
   95000.00,
   'QR-SEED-METALLICA-001', 'VENDIDA', '2026-10-01 14:06:00.000', NULL),
  ('aa000000-0000-4000-8000-000000000002',
   'f0000000-0000-4000-8000-000000000043',
   'b0000000-0000-4000-8000-000000000001',
   'd0000000-0000-4000-8000-000000000003',
   'c0000000-0000-4000-8000-000000000013',
   75000.00,
   'QR-SEED-CANCEL-RIR-001', 'VENDIDA', '2026-09-15 10:06:00.000', NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado), precio = VALUES(precio), codigo_qr = VALUES(codigo_qr);

-- Devolución pendiente de ejemplo (sobre entrada Metallica) — cola admin
INSERT INTO devolucion (id_devolucion, id_entrada, motivo, monto, fecha_solicitud, estado) VALUES
  ('aa000000-0000-4000-8000-000000000011',
   'aa000000-0000-4000-8000-000000000001',
   'Cambio de planes (seed demo)', 95000.00, '2026-10-10 09:00:00.000', 'PENDIENTE')
ON DUPLICATE KEY UPDATE estado = VALUES(estado), monto = VALUES(monto);

-- Sin validaciones seed (tabla vacía de hechos; estructura lista)
