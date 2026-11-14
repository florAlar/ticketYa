-- msvc-notificaciones | DB: notificaciones
-- Schema + seed mínimo (plantilla). id_usuario / id_evento_origen = refs lógicas.
-- IDs fijos: infrastructure/mysql/README.md

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS plantilla_notificacion (
  id_plantilla CHAR(36)     NOT NULL,
  tipo_evento  VARCHAR(64)  NOT NULL,
  canal        VARCHAR(32)  NOT NULL,
  asunto       VARCHAR(255) NOT NULL,
  contenido    TEXT         NOT NULL,
  activa       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notificacion (
  id_notificacion  CHAR(36)     NOT NULL,
  id_usuario       CHAR(36)     NOT NULL COMMENT 'ref lógica usuarios.usuario',
  id_evento_origen CHAR(36)     NULL COMMENT 'ref lógica eventos.evento',
  id_plantilla     CHAR(36)     NULL,
  tipo             VARCHAR(64)  NOT NULL,
  canal            VARCHAR(32)  NOT NULL,
  asunto           VARCHAR(255) NOT NULL,
  contenido        TEXT         NOT NULL,
  fecha_creacion   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  fecha_envio      DATETIME(3)  NULL,
  estado           VARCHAR(32)  NOT NULL,
  PRIMARY KEY (id_notificacion),
  KEY ix_notif_usuario (id_usuario),
  CONSTRAINT fk_notif_plantilla FOREIGN KEY (id_plantilla) REFERENCES plantilla_notificacion (id_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS intento_envio (
  id_intento       CHAR(36)     NOT NULL,
  id_notificacion  CHAR(36)     NOT NULL,
  numero_intento   INT          NOT NULL DEFAULT 1,
  fecha_intento    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  resultado        VARCHAR(32)  NOT NULL,
  detalle_error    VARCHAR(500) NULL,
  PRIMARY KEY (id_intento),
  CONSTRAINT fk_intento_notif FOREIGN KEY (id_notificacion) REFERENCES notificacion (id_notificacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO plantilla_notificacion (id_plantilla, tipo_evento, canal, asunto, contenido, activa) VALUES
  ('n0000000-0000-4000-8000-000000000001', 'CompraConfirmada', 'EMAIL',
   'Tu compra en TicketYa', 'Hola, adjuntamos el detalle de tu compra y entradas.', 1),
  ('n0000000-0000-4000-8000-000000000002', 'EventoCancelado', 'PUSH',
   'Evento cancelado', 'El evento fue cancelado. Gestionaremos el reintegro.', 1)
ON DUPLICATE KEY UPDATE asunto = VALUES(asunto), canal = VALUES(canal), contenido = VALUES(contenido);

-- Sin notificaciones/intentos seed (solo catálogo de plantillas)
