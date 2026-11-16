-- Migración para volúmenes ya creados (init.sql solo corre en DB vacía).
-- Envío dual (PUSH + EMAIL) para avisos de evento: agrega plantillas EMAIL equivalentes.

INSERT INTO plantilla_notificacion (id_plantilla, tipo_evento, canal, asunto, contenido, activa) VALUES
  ('n0000000-0000-4000-8000-000000000006', 'EventoCancelado', 'EMAIL',
   'Evento cancelado',
   'El evento {{evento}} del {{fecha}} fue cancelado. Gestionaremos el reintegro.', 1),
  ('n0000000-0000-4000-8000-000000000007', 'EventoSuspendido', 'EMAIL',
   'Evento suspendido',
   'El evento {{evento}} del {{fecha}} fue suspendido. Te avisaremos cuando se reprograme.', 1),
  ('n0000000-0000-4000-8000-000000000008', 'EventoModificado', 'EMAIL',
   'Cambió un evento que te interesa',
   'El evento {{evento}} cambió: {{cambios}}. Nueva fecha: {{fecha}}.', 1),
  ('n0000000-0000-4000-8000-000000000009', 'NuevoEventoArtistaFavorito', 'EMAIL',
   'Tu artista favorito se presenta',
   '¡{{evento}} en {{ciudad}}! Un artista que seguís se presenta el {{fecha}}.', 1)
ON DUPLICATE KEY UPDATE
  tipo_evento = VALUES(tipo_evento), canal = VALUES(canal), asunto = VALUES(asunto),
  contenido = VALUES(contenido), activa = VALUES(activa);
