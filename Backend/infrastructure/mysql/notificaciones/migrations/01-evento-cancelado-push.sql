-- Migración para volúmenes ya creados (init.sql solo corre en DB vacía).
-- Cambia la plantilla de cancelación de evento al canal PUSH usado por el consumidor.

INSERT INTO plantilla_notificacion (id_plantilla, tipo_evento, canal, asunto, contenido, activa)
VALUES ('n0000000-0000-4000-8000-000000000002', 'EventoCancelado', 'PUSH',
        'Evento cancelado', 'El evento fue cancelado. Gestionaremos el reintegro.', 1)
ON DUPLICATE KEY UPDATE canal = 'PUSH', asunto = VALUES(asunto), contenido = VALUES(contenido), activa = 1;

UPDATE plantilla_notificacion
SET canal = 'PUSH'
WHERE tipo_evento = 'EventoCancelado'
  AND canal <> 'PUSH';
