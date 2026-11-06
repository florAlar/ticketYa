-- msvc-usuarios | DB: usuarios
-- Schema + seed mínimo (modelo relacional TicketYa).
-- IDs fijos para referencias lógicas cross-msvc (ver infrastructure/mysql/README.md).
-- Contraseñas seed (BCrypt): demo1234 / org1234 / admin1234 / acceso1234

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS rol (
  id_rol        CHAR(36)     NOT NULL,
  nombre        VARCHAR(64)  NOT NULL,
  PRIMARY KEY (id_rol),
  UNIQUE KEY uk_rol_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario     CHAR(36)     NOT NULL,
  nombre         VARCHAR(100) NOT NULL,
  apellido       VARCHAR(100) NOT NULL DEFAULT '',
  email          VARCHAR(255) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  ciudad         VARCHAR(120) NULL,
  fecha_registro DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  activo         TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuario_rol (
  id_usuario CHAR(36) NOT NULL,
  id_rol     CHAR(36) NOT NULL,
  PRIMARY KEY (id_usuario, id_rol),
  CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  CONSTRAINT fk_usuario_rol_rol     FOREIGN KEY (id_rol)     REFERENCES rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Roles canónicos (endpoints-back)
INSERT INTO rol (id_rol, nombre) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'CLIENTE'),
  ('a0000000-0000-4000-8000-000000000002', 'ORGANIZADOR'),
  ('a0000000-0000-4000-8000-000000000003', 'ADMIN'),
  ('a0000000-0000-4000-8000-000000000004', 'PERSONAL_ACCESO')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO usuario (id_usuario, nombre, apellido, email, password_hash, ciudad, activo) VALUES
  ('b0000000-0000-4000-8000-000000000001', 'Usuario', 'Demo',  'demo@ticketya.com',
   '$2b$10$ObI../Q7KOx.axynvCYGNu/EYUEvAXD9LD3.tNnoTiGcyAjU.E2J6', 'Tandil', 1),
  ('b0000000-0000-4000-8000-000000000002', 'Org',     'Demo',  'org@ticketya.com',
   '$2b$10$CSRgH3jW4WQhtaYtR4jGxuhAMxfxrdbnt2/cEF11yUfjpHdBg132K', 'Tandil', 1),
  ('b0000000-0000-4000-8000-000000000003', 'Admin',   'Demo',  'admin@ticketya.com',
   '$2b$10$o13AtD5dLOgc0.6O3IFE5eTl6OXpdfxaP2YwbR0rdqD06OJKnV8w.', 'Tandil', 1),
  ('b0000000-0000-4000-8000-000000000004', 'Acceso',  'Demo',  'acceso@ticketya.com',
   '$2b$10$GwrqHURKocS4QU7drmUL9O2dq9PT6SylDX2b2dVIg5jE.cEMmPaJS', 'Tandil', 1)
ON DUPLICATE KEY UPDATE email = VALUES(email);

INSERT INTO usuario_rol (id_usuario, id_rol) VALUES
  ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001'),
  ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002'),
  ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000003'),
  ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000004')
ON DUPLICATE KEY UPDATE id_rol = VALUES(id_rol);
