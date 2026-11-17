-- Migración para volúmenes ya creados (init.sql solo corre en DB vacía).
-- Plantillas PUSH + EMAIL del aviso Devolución aprobada (evento devolucion.aprobada).

INSERT INTO plantilla_notificacion (id_plantilla, tipo_evento, canal, asunto, contenido, activa) VALUES
  ('n0000000-0000-4000-8000-00000000000a', 'DevolucionAprobada', 'PUSH',
   'Devolución aprobada',
   'Aprobamos la devolución de tu entrada para el evento {{eventoId}}. Reintegro: {{monto}} {{moneda}}.', 1),
  ('n0000000-0000-4000-8000-00000000000b', 'DevolucionAprobada', 'EMAIL',
   'Devolución aprobada',
   'Aprobamos la devolución de tu entrada para el evento {{eventoId}}. Reintegro: {{monto}} {{moneda}}.', 1)
ON DUPLICATE KEY UPDATE
  tipo_evento = VALUES(tipo_evento), canal = VALUES(canal), asunto = VALUES(asunto),
  contenido = VALUES(contenido), activa = VALUES(activa);
