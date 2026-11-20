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
  latitud          DECIMAL(9, 6) NULL COMMENT 'grados decimales (-90..90); opcional, va junto con longitud',
  longitud         DECIMAL(9, 6) NULL COMMENT 'grados decimales (-180..180); opcional, va junto con latitud',
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
  filas             INT    NULL COMMENT 'solo sector numerado: cantidad de filas (A, B, ... AA)',
  asientos_por_fila INT    NULL COMMENT 'solo sector numerado: asientos por fila; capacidad = filas * asientos_por_fila',
  PRIMARY KEY (id),
  CONSTRAINT fk_sector_lugar FOREIGN KEY (id_lugar) REFERENCES lugar (id)
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
  imagen_url         VARCHAR(500) NULL COMMENT 'portada: URL http(s) o ruta relativa que sirve el front; NULL = portada generada',
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
  cupo      INT            NULL COMMENT 'cupo comercial del evento en el sector; NULL = toda la capacidad del sector',
  PRIMARY KEY (id),
  UNIQUE KEY uk_precio_evento_sector (id_evento, id_sector),
  CONSTRAINT fk_precio_sector_evento FOREIGN KEY (id_evento) REFERENCES evento (id),
  CONSTRAINT fk_precio_sector_sector FOREIGN KEY (id_sector) REFERENCES sector (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Artistas/bandas del show (no compradores). N:M con evento.
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

INSERT INTO lugar (id, nombre, direccion, ciudad, latitud, longitud, capacidad_total) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'Estadio Más Monumental', 'Av. Figueroa Alcorta 7597', 'Buenos Aires', -34.545300, -58.449700, 85000),
  ('c0000000-0000-4000-8000-000000000002', 'Estádio do Maracanã', 'Av. Pres. Castelo Branco', 'Rio de Janeiro', -22.912200, -43.230200, 78838),
  ('c0000000-0000-4000-8000-000000000003', 'Wembley Stadium', 'Wembley', 'London', 51.556000, -0.279600, 90000)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), direccion = VALUES(direccion), ciudad = VALUES(ciudad), latitud = VALUES(latitud), longitud = VALUES(longitud), capacidad_total = VALUES(capacidad_total);

INSERT INTO sector (id, id_lugar, nombre, capacidad, tipo, es_numerado, filas, asientos_por_fila) VALUES
  ('c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000001', 'Campo', 60000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000001', 'Platea', 25000, 'PLATEA', 1, 100, 250),
  ('c0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-000000000002', 'Arquibancada', 55000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-000000000014', 'c0000000-0000-4000-8000-000000000002', 'Cadeira', 20000, 'PLATEA', 1, 80, 250),
  ('c0000000-0000-4000-8000-000000000015', 'c0000000-0000-4000-8000-000000000003', 'General Admission', 65000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-000000000016', 'c0000000-0000-4000-8000-000000000003', 'Club Wembley', 18000, 'PLATEA', 1, 72, 250)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), capacidad = VALUES(capacidad), tipo = VALUES(tipo),
  filas = VALUES(filas), asientos_por_fila = VALUES(asientos_por_fila);

-- Catálogo demo amplio (IDs d001–d003 fijos cross-msvc compras/entradas)
INSERT INTO evento (id, id_organizador, nombre, descripcion, tipo, fecha_inicio, fecha_fin, estado, id_lugar, fecha_publicacion, es_alta_demanda) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
   'Metallica — M72 World Tour', 'Gira mundial M72. Headliner Metallica con soporte Gojira.', 'RECITAL',
   '2027-03-15 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-01 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002',
   'UEFA Champions League Final 2027', 'Final de la Champions League. Venta suspendida temporalmente.', 'DEPORTE',
   '2027-05-30 21:00:00.000', NULL, 'SUSPENDIDO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-01 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000002',
   'Rock in Rio — día metal (cancelado)', 'Jornada metal cancelada por la organización. Reintegros activos.', 'FESTIVAL',
   '2027-09-18 16:00:00.000', NULL, 'CANCELADO',
   'c0000000-0000-4000-8000-000000000002', '2026-09-01 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000002',
   'Iron Maiden — The Future Past Tour', 'Clásicos de Powerslave y Senjutsu en vivo.', 'RECITAL',
   '2027-04-10 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-05 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000002',
   'Rammstein — Stadium Tour', 'Producción pirotécnica a estadio completo.', 'RECITAL',
   '2027-04-22 21:30:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-05 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000002',
   'Foo Fighters — Everything or Nothing at All', 'Rock stadium show.', 'RECITAL',
   '2027-06-12 20:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000003', '2026-10-08 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002',
   'Coldplay — Music of the Spheres', 'World tour con producción LED y confeti.', 'RECITAL',
   '2027-07-03 20:30:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000003', '2026-10-08 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000002',
   'Formula 1 — São Paulo Grand Prix', 'GP de Brasil, autódromo Interlagos (pase estadio fan zone).', 'DEPORTE',
   '2027-11-07 14:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000002', '2026-10-10 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000002',
   'NBA Global Games — Lakers vs Celtics', 'Partido de pretemporada internacional.', 'DEPORTE',
   '2027-10-05 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000003', '2026-10-10 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-000000000002',
   'Slipknot — Here Comes the Pain', 'Núcleo metal extremo en estadio.', 'RECITAL',
   '2027-08-20 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-12 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-00000000000b', 'b0000000-0000-4000-8000-000000000002',
   'Download Festival — Main Stage Sunday', 'Día principal del festival de rock/metal.', 'FESTIVAL',
   '2027-06-14 12:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000003', '2026-10-12 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-00000000000c', 'b0000000-0000-4000-8000-000000000002',
   'Oasis — Live ''25', 'Reencuentro. Entradas agotadas.', 'RECITAL',
   '2027-07-25 20:00:00.000', NULL, 'AGOTADO',
   'c0000000-0000-4000-8000-000000000003', '2026-09-20 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-00000000000d', 'b0000000-0000-4000-8000-000000000002',
   'Tool — Fear Inoculum Tour', 'Progressive metal, setlist extendido.', 'RECITAL',
   '2027-05-08 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-15 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-00000000000e', 'b0000000-0000-4000-8000-000000000002',
   'FIFA World Cup Qualifier — Brazil vs Argentina', 'Clásico sudamericano en el Maracaná.', 'DEPORTE',
   '2027-09-05 16:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000002', '2026-10-15 12:00:00.000', 1)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  descripcion = VALUES(descripcion),
  tipo = VALUES(tipo),
  fecha_inicio = VALUES(fecha_inicio),
  estado = VALUES(estado),
  id_lugar = VALUES(id_lugar),
  es_alta_demanda = VALUES(es_alta_demanda);

-- Portadas demo: archivos en Frontend/public/img/eventos (los sirve el nginx del front).
-- Solo los eventos que tienen una foto que les corresponde; el resto queda NULL y el front
-- muestra una portada generada.
UPDATE evento SET imagen_url = '/img/eventos/metallica.webp'     WHERE id = 'd0000000-0000-4000-8000-000000000001';
UPDATE evento SET imagen_url = '/img/eventos/festival-crowd.webp' WHERE id = 'd0000000-0000-4000-8000-000000000003';
UPDATE evento SET imagen_url = '/img/eventos/iron-maiden.webp'   WHERE id = 'd0000000-0000-4000-8000-000000000004';
UPDATE evento SET imagen_url = '/img/eventos/rammstein.webp'     WHERE id = 'd0000000-0000-4000-8000-000000000005';
UPDATE evento SET imagen_url = '/img/eventos/foo-fighters.webp'  WHERE id = 'd0000000-0000-4000-8000-000000000006';
UPDATE evento SET imagen_url = '/img/eventos/festival-crowd.webp' WHERE id = 'd0000000-0000-4000-8000-00000000000b';
UPDATE evento SET imagen_url = '/img/eventos/tool.webp'          WHERE id = 'd0000000-0000-4000-8000-00000000000d';

INSERT INTO precio_sector (id, id_evento, id_sector, precio, moneda, activo) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011', 95000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000012', 180000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000011', 150000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000012', 280000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000013', 75000.00, 'ARS', 0),
  ('e0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000011', 88000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000012', 160000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000011', 110000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000012', 195000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000000a', 'd0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000015', 120000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000000b', 'd0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000016', 210000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000000c', 'd0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000015', 135000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000000d', 'd0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000016', 240000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000000e', 'd0000000-0000-4000-8000-000000000008', 'c0000000-0000-4000-8000-000000000013', 175000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000000f', 'd0000000-0000-4000-8000-000000000008', 'c0000000-0000-4000-8000-000000000014', 320000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000010', 'd0000000-0000-4000-8000-000000000009', 'c0000000-0000-4000-8000-000000000015', 98000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000011', 'd0000000-0000-4000-8000-000000000009', 'c0000000-0000-4000-8000-000000000016', 185000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000012', 'd0000000-0000-4000-8000-00000000000a', 'c0000000-0000-4000-8000-000000000011', 72000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000013', 'd0000000-0000-4000-8000-00000000000a', 'c0000000-0000-4000-8000-000000000012', 140000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000014', 'd0000000-0000-4000-8000-00000000000b', 'c0000000-0000-4000-8000-000000000015', 85000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000015', 'd0000000-0000-4000-8000-00000000000b', 'c0000000-0000-4000-8000-000000000016', 155000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000016', 'd0000000-0000-4000-8000-00000000000c', 'c0000000-0000-4000-8000-000000000015', 200000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000017', 'd0000000-0000-4000-8000-00000000000c', 'c0000000-0000-4000-8000-000000000016', 350000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000018', 'd0000000-0000-4000-8000-00000000000d', 'c0000000-0000-4000-8000-000000000011', 90000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000019', 'd0000000-0000-4000-8000-00000000000d', 'c0000000-0000-4000-8000-000000000012', 170000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000001a', 'd0000000-0000-4000-8000-00000000000e', 'c0000000-0000-4000-8000-000000000013', 125000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000001b', 'd0000000-0000-4000-8000-00000000000e', 'c0000000-0000-4000-8000-000000000014', 250000.00, 'ARS', 1)
ON DUPLICATE KEY UPDATE precio = VALUES(precio), activo = VALUES(activo);

INSERT INTO participante (id, nombre, tipo, descripcion) VALUES
  ('p0000000-0000-4000-8000-000000000001', 'Metallica', 'BANDA', 'Thrash metal — M72 World Tour'),
  ('p0000000-0000-4000-8000-000000000002', 'Gojira', 'BANDA', 'Soporte M72'),
  ('p0000000-0000-4000-8000-000000000003', 'Real Madrid CF', 'EQUIPO', 'Finalista Champions League'),
  ('p0000000-0000-4000-8000-000000000004', 'Manchester City FC', 'EQUIPO', 'Finalista Champions League'),
  ('p0000000-0000-4000-8000-000000000005', 'Iron Maiden', 'BANDA', 'The Future Past / festivals'),
  ('p0000000-0000-4000-8000-000000000006', 'Ghost', 'BANDA', 'Soporte / co-bill'),
  ('p0000000-0000-4000-8000-000000000007', 'Rammstein', 'BANDA', 'Industrial metal stadium'),
  ('p0000000-0000-4000-8000-000000000008', 'Foo Fighters', 'BANDA', 'Alternative rock'),
  ('p0000000-0000-4000-8000-000000000009', 'Coldplay', 'BANDA', 'Pop rock stadium'),
  ('p0000000-0000-4000-8000-00000000000a', 'Los Angeles Lakers', 'EQUIPO', 'NBA'),
  ('p0000000-0000-4000-8000-00000000000b', 'Boston Celtics', 'EQUIPO', 'NBA'),
  ('p0000000-0000-4000-8000-00000000000c', 'Slipknot', 'BANDA', 'Nu metal / alternative metal'),
  ('p0000000-0000-4000-8000-00000000000d', 'Bring Me The Horizon', 'BANDA', 'Download co-headliner'),
  ('p0000000-0000-4000-8000-00000000000e', 'Oasis', 'BANDA', 'Britpop reunion'),
  ('p0000000-0000-4000-8000-00000000000f', 'Tool', 'BANDA', 'Progressive metal'),
  ('p0000000-0000-4000-8000-000000000010', 'Selección Brasil', 'EQUIPO', 'FIFA'),
  ('p0000000-0000-4000-8000-000000000011', 'Selección Argentina', 'EQUIPO', 'FIFA'),
  ('p0000000-0000-4000-8000-000000000012', 'Max Verstappen', 'PILOTO', 'F1 Red Bull')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), tipo = VALUES(tipo), descripcion = VALUES(descripcion);

INSERT INTO evento_participante (id_evento, id_participante, rol, orden) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'p0000000-0000-4000-8000-000000000001', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000001', 'p0000000-0000-4000-8000-000000000002', 'TELONERO', 1),
  ('d0000000-0000-4000-8000-000000000002', 'p0000000-0000-4000-8000-000000000003', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000002', 'p0000000-0000-4000-8000-000000000004', 'PRINCIPAL', 1),
  ('d0000000-0000-4000-8000-000000000003', 'p0000000-0000-4000-8000-000000000005', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000003', 'p0000000-0000-4000-8000-000000000006', 'TELONERO', 1),
  ('d0000000-0000-4000-8000-000000000004', 'p0000000-0000-4000-8000-000000000005', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000005', 'p0000000-0000-4000-8000-000000000007', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000006', 'p0000000-0000-4000-8000-000000000008', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000007', 'p0000000-0000-4000-8000-000000000009', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000008', 'p0000000-0000-4000-8000-000000000012', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000009', 'p0000000-0000-4000-8000-00000000000a', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000009', 'p0000000-0000-4000-8000-00000000000b', 'PRINCIPAL', 1),
  ('d0000000-0000-4000-8000-00000000000a', 'p0000000-0000-4000-8000-00000000000c', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-00000000000b', 'p0000000-0000-4000-8000-000000000005', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-00000000000b', 'p0000000-0000-4000-8000-00000000000d', 'PRINCIPAL', 1),
  ('d0000000-0000-4000-8000-00000000000c', 'p0000000-0000-4000-8000-00000000000e', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-00000000000d', 'p0000000-0000-4000-8000-00000000000f', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-00000000000e', 'p0000000-0000-4000-8000-000000000010', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-00000000000e', 'p0000000-0000-4000-8000-000000000011', 'PRINCIPAL', 1)
ON DUPLICATE KEY UPDATE rol = VALUES(rol), orden = VALUES(orden);

-- Bandas del hero de la home: Ghost, Turmion Kätilöt y Red Hot Chili Peppers.
INSERT INTO evento (id, id_organizador, nombre, descripcion, tipo, fecha_inicio, fecha_fin, estado, id_lugar, fecha_publicacion, es_alta_demanda) VALUES
  ('d0000000-0000-4000-8000-00000000000f', 'b0000000-0000-4000-8000-000000000002',
   'Ghost — Skeletour Latinoamérica', 'Rock teatral y liturgia pesada: la gira de Ghost llega con su puesta completa.', 'RECITAL',
   '2027-09-12 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000003', '2026-10-18 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000002',
   'Turmion Kätilöt — Industrial Metal Night', 'Industrial metal finlandés: riffs pesados, electrónica y un show a puro ritual.', 'RECITAL',
   '2027-10-17 20:30:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000002', '2026-10-18 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000002',
   'Red Hot Chili Peppers — Unlimited Love Tour', 'Funk rock californiano a estadio lleno, con todos sus clásicos.', 'RECITAL',
   '2027-11-20 21:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000001', '2026-10-20 12:00:00.000', 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion), tipo = VALUES(tipo),
  fecha_inicio = VALUES(fecha_inicio), estado = VALUES(estado), id_lugar = VALUES(id_lugar), es_alta_demanda = VALUES(es_alta_demanda);

UPDATE evento SET imagen_url = '/img/eventos/ghost.webp'                  WHERE id = 'd0000000-0000-4000-8000-00000000000f';
UPDATE evento SET imagen_url = '/img/eventos/turmion-katilot.webp'         WHERE id = 'd0000000-0000-4000-8000-000000000010';
UPDATE evento SET imagen_url = '/img/eventos/red-hot-chili-peppers.webp'  WHERE id = 'd0000000-0000-4000-8000-000000000011';
UPDATE evento SET imagen_url = '/img/eventos/slipknot.webp'               WHERE id = 'd0000000-0000-4000-8000-00000000000a';

INSERT INTO precio_sector (id, id_evento, id_sector, precio, moneda, activo) VALUES
  ('e0000000-0000-4000-8000-00000000001c', 'd0000000-0000-4000-8000-00000000000f', 'c0000000-0000-4000-8000-000000000015', 98000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000001d', 'd0000000-0000-4000-8000-00000000000f', 'c0000000-0000-4000-8000-000000000016', 175000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000001e', 'd0000000-0000-4000-8000-000000000010', 'c0000000-0000-4000-8000-000000000013', 82000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-00000000001f', 'd0000000-0000-4000-8000-000000000010', 'c0000000-0000-4000-8000-000000000014', 150000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000020', 'd0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000011', 130000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000021', 'd0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000012', 230000.00, 'ARS', 1)
ON DUPLICATE KEY UPDATE precio = VALUES(precio), activo = VALUES(activo);

INSERT INTO participante (id, nombre, tipo, descripcion) VALUES
  ('p0000000-0000-4000-8000-000000000013', 'Turmion Kätilöt', 'BANDA', 'Industrial metal'),
  ('p0000000-0000-4000-8000-000000000014', 'Red Hot Chili Peppers', 'BANDA', 'Funk rock')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), tipo = VALUES(tipo), descripcion = VALUES(descripcion);

INSERT INTO evento_participante (id_evento, id_participante, rol, orden) VALUES
  ('d0000000-0000-4000-8000-00000000000f', 'p0000000-0000-4000-8000-000000000006', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000010', 'p0000000-0000-4000-8000-000000000013', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000011', 'p0000000-0000-4000-8000-000000000014', 'PRINCIPAL', 0)
ON DUPLICATE KEY UPDATE rol = VALUES(rol), orden = VALUES(orden);

-- Eventos deportivos del hero: Boca (Superclásico), Dakar, MotoGP y UFC (la Fórmula 1 ya existía).
INSERT INTO lugar (id, nombre, direccion, ciudad, latitud, longitud, capacidad_total) VALUES
  ('c0000000-0000-4000-8000-000000000004', 'La Bombonera', 'Brandsen 805', 'Buenos Aires', -34.635600, -58.364700, 50000),
  ('c0000000-0000-4000-8000-000000000005', 'Autódromo Termas de Río Hondo', 'Ruta 9 km 1015', 'Santiago del Estero', -27.481700, -64.934200, 50000),
  ('c0000000-0000-4000-8000-000000000006', 'Movistar Arena', 'Humboldt 450', 'Buenos Aires', -34.593100, -58.447800, 15000),
  ('c0000000-0000-4000-8000-000000000007', 'Campamento Base Dakar', 'Zona de largada', 'Riad', 24.713600, 46.675300, 20000)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), direccion = VALUES(direccion), ciudad = VALUES(ciudad), latitud = VALUES(latitud), longitud = VALUES(longitud), capacidad_total = VALUES(capacidad_total);

INSERT INTO sector (id, id_lugar, nombre, capacidad, tipo, es_numerado, filas, asientos_por_fila) VALUES
  ('c0000000-0000-4000-8000-000000000017', 'c0000000-0000-4000-8000-000000000004', 'Popular', 30000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-000000000018', 'c0000000-0000-4000-8000-000000000004', 'Platea', 20000, 'PLATEA', 1, 80, 250),
  ('c0000000-0000-4000-8000-000000000019', 'c0000000-0000-4000-8000-000000000005', 'Tribuna general', 40000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-00000000001a', 'c0000000-0000-4000-8000-000000000005', 'Tribuna preferencial', 10000, 'PLATEA', 1, 40, 250),
  ('c0000000-0000-4000-8000-00000000001b', 'c0000000-0000-4000-8000-000000000006', 'Cancha', 6000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-00000000001c', 'c0000000-0000-4000-8000-000000000006', 'Platea', 9000, 'PLATEA', 1, 36, 250),
  ('c0000000-0000-4000-8000-00000000001d', 'c0000000-0000-4000-8000-000000000007', 'General', 15000, 'GENERAL', 0, NULL, NULL),
  ('c0000000-0000-4000-8000-00000000001e', 'c0000000-0000-4000-8000-000000000007', 'VIP', 5000, 'PLATEA', 1, 20, 250)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), capacidad = VALUES(capacidad), tipo = VALUES(tipo),
  filas = VALUES(filas), asientos_por_fila = VALUES(asientos_por_fila);

INSERT INTO evento (id, id_organizador, nombre, descripcion, tipo, fecha_inicio, fecha_fin, estado, id_lugar, fecha_publicacion, es_alta_demanda) VALUES
  ('d0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000002',
   'Boca Juniors vs River Plate — Superclásico', 'El clásico más grande del fútbol argentino, en La Bombonera.', 'DEPORTE',
   '2027-08-29 17:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000004', '2026-10-22 12:00:00.000', 1),
  ('d0000000-0000-4000-8000-000000000013', 'b0000000-0000-4000-8000-000000000002',
   'Dakar Rally — Largada y Prólogo', 'La largada del rally más duro del mundo: autos, motos y camiones en el desierto.', 'DEPORTE',
   '2028-01-12 10:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000007', '2026-10-22 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000014', 'b0000000-0000-4000-8000-000000000002',
   'MotoGP — Gran Premio de Argentina', 'La máxima categoría del motociclismo en Termas de Río Hondo.', 'DEPORTE',
   '2027-04-02 14:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000005', '2026-10-24 12:00:00.000', 0),
  ('d0000000-0000-4000-8000-000000000015', 'b0000000-0000-4000-8000-000000000002',
   'UFC Fight Night — Buenos Aires', 'Una noche de artes marciales mixtas con las mejores peleas del octágono.', 'DEPORTE',
   '2027-09-18 20:00:00.000', NULL, 'PUBLICADO',
   'c0000000-0000-4000-8000-000000000006', '2026-10-24 12:00:00.000', 0)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion), tipo = VALUES(tipo),
  fecha_inicio = VALUES(fecha_inicio), estado = VALUES(estado), id_lugar = VALUES(id_lugar), es_alta_demanda = VALUES(es_alta_demanda);

UPDATE evento SET imagen_url = '/img/eventos/formula-1.webp'    WHERE id = 'd0000000-0000-4000-8000-000000000008';
UPDATE evento SET imagen_url = '/img/eventos/boca-vs-river.webp' WHERE id = 'd0000000-0000-4000-8000-000000000012';
UPDATE evento SET imagen_url = '/img/eventos/dakar-rally.webp'  WHERE id = 'd0000000-0000-4000-8000-000000000013';
UPDATE evento SET imagen_url = '/img/eventos/motogp.webp'       WHERE id = 'd0000000-0000-4000-8000-000000000014';
UPDATE evento SET imagen_url = '/img/eventos/ufc.webp'          WHERE id = 'd0000000-0000-4000-8000-000000000015';

INSERT INTO precio_sector (id, id_evento, id_sector, precio, moneda, activo) VALUES
  ('e0000000-0000-4000-8000-000000000022', 'd0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000017', 60000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000023', 'd0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000018', 140000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000024', 'd0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-00000000001d', 70000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000025', 'd0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-00000000001e', 160000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000026', 'd0000000-0000-4000-8000-000000000014', 'c0000000-0000-4000-8000-000000000019', 80000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000027', 'd0000000-0000-4000-8000-000000000014', 'c0000000-0000-4000-8000-00000000001a', 180000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000028', 'd0000000-0000-4000-8000-000000000015', 'c0000000-0000-4000-8000-00000000001b', 90000.00, 'ARS', 1),
  ('e0000000-0000-4000-8000-000000000029', 'd0000000-0000-4000-8000-000000000015', 'c0000000-0000-4000-8000-00000000001c', 170000.00, 'ARS', 1)
ON DUPLICATE KEY UPDATE precio = VALUES(precio), activo = VALUES(activo);

INSERT INTO participante (id, nombre, tipo, descripcion) VALUES
  ('p0000000-0000-4000-8000-000000000015', 'Boca Juniors', 'EQUIPO', 'Fútbol argentino'),
  ('p0000000-0000-4000-8000-000000000016', 'River Plate', 'EQUIPO', 'Fútbol argentino'),
  ('p0000000-0000-4000-8000-000000000017', 'Dakar Rally', 'ORGANIZACION', 'Rally raid'),
  ('p0000000-0000-4000-8000-000000000018', 'MotoGP', 'ORGANIZACION', 'Motociclismo de velocidad'),
  ('p0000000-0000-4000-8000-000000000019', 'UFC', 'ORGANIZACION', 'Artes marciales mixtas')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), tipo = VALUES(tipo), descripcion = VALUES(descripcion);

INSERT INTO evento_participante (id_evento, id_participante, rol, orden) VALUES
  ('d0000000-0000-4000-8000-000000000012', 'p0000000-0000-4000-8000-000000000015', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000012', 'p0000000-0000-4000-8000-000000000016', 'PRINCIPAL', 1),
  ('d0000000-0000-4000-8000-000000000013', 'p0000000-0000-4000-8000-000000000017', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000014', 'p0000000-0000-4000-8000-000000000018', 'PRINCIPAL', 0),
  ('d0000000-0000-4000-8000-000000000015', 'p0000000-0000-4000-8000-000000000019', 'PRINCIPAL', 0)
ON DUPLICATE KEY UPDATE rol = VALUES(rol), orden = VALUES(orden);


-- Portadas externas (URL absoluta; evento.imagen_url admite http/https) para los eventos sin foto propia.
-- Fotos de Wikimedia Commons (licencia libre).
UPDATE evento SET imagen_url = 'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/14/Coldplay_at_Craven_Park_Stadium_Stage_View%2C_Kingston_upon_Hull_18-08-2025.jpg/1280px-Coldplay_at_Craven_Park_Stadium_Stage_View%2C_Kingston_upon_Hull_18-08-2025.jpg' WHERE id = 'd0000000-0000-4000-8000-000000000007';
UPDATE evento SET imagen_url = 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/df/Oasis-band-concert-Montreal-Canada-Aug2002.jpg/1280px-Oasis-band-concert-Montreal-Canada-Aug2002.jpg' WHERE id = 'd0000000-0000-4000-8000-00000000000c';
UPDATE evento SET imagen_url = 'https://upload.wikimedia.org/wikipedia/commons/b/bd/Basketball_arena_match_sport_game_lakers_tribune_fans-1223638.jpg' WHERE id = 'd0000000-0000-4000-8000-000000000009';
UPDATE evento SET imagen_url = 'https://upload.wikimedia.org/wikipedia/commons/6/69/Khalifa_Stadium%2C_Doha%2C_Brazil_vs_Argentina_%282010%29.jpg' WHERE id = 'd0000000-0000-4000-8000-00000000000e';
UPDATE evento SET imagen_url = 'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/78/UEFA_Champions_League_Final_Cardiff_2017_adjusted.jpg/1280px-UEFA_Champions_League_Final_Cardiff_2017_adjusted.jpg' WHERE id = 'd0000000-0000-4000-8000-000000000002';
