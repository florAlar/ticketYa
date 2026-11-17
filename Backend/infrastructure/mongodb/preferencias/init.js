// msvc-preferencias | DB: preferencias
// Colecciones + índices + seed mínimo según docs/diagramas/modelo-mongodb.md
// IDs lógicos alineados a infrastructure/mysql/README.md (usuario demo, eventos seed).
// Corre solo en el primer arranque del volumen (docker-entrypoint-initdb.d).
// Para un volumen ya creado usar scripts/migracion-preferencias-e2.js.

const dbName = 'preferencias';
const prefDb = db.getSiblingDB(dbName);

// --- Colecciones (idempotente: también sirve para migrar un volumen existente) ---
const existentes = prefDb.getCollectionNames();
for (const coleccion of ['preferencias_clientes', 'interacciones_eventos']) {
  if (!existentes.includes(coleccion)) {
    prefDb.createCollection(coleccion);
  }
}

// --- Índices ---
prefDb.preferencias_clientes.createIndex(
  { idCliente: 1 },
  { unique: true, name: 'uk_preferencias_idCliente' }
);

prefDb.interacciones_eventos.createIndex(
  { idCliente: 1 },
  { name: 'ix_interacciones_idCliente' }
);
prefDb.interacciones_eventos.createIndex(
  { idEvento: 1 },
  { name: 'ix_interacciones_idEvento' }
);
prefDb.interacciones_eventos.createIndex(
  { idCliente: 1, fechaEvento: -1 },
  { name: 'ix_interacciones_cliente_fecha' }
);
// E2-PRE: ingesta idempotente y consultas de co-compra.
prefDb.interacciones_eventos.createIndex(
  { idCompra: 1 },
  { unique: true, sparse: true, name: 'uk_interacciones_idCompra' }
);
prefDb.interacciones_eventos.createIndex(
  { idEvento: 1, tipo: 1 },
  { name: 'ix_interacciones_evento_tipo' }
);
prefDb.interacciones_eventos.createIndex(
  { idCliente: 1, tipo: 1 },
  { name: 'ix_interacciones_cliente_tipo' }
);

// --- Seed: preferencias del cliente demo ---
const ID_CLIENTE_DEMO = 'b0000000-0000-4000-8000-000000000001';
const ID_EVENTO_METALLICA = 'd0000000-0000-4000-8000-000000000001';
const ID_EVENTO_UCL = 'd0000000-0000-4000-8000-000000000002';
const ID_EVENTO_ROCK_IN_RIO = 'd0000000-0000-4000-8000-000000000003';

prefDb.preferencias_clientes.updateOne(
  { idCliente: ID_CLIENTE_DEMO },
  {
    $set: {
      idCliente: ID_CLIENTE_DEMO,
      categorias: ['RECITAL', 'DEPORTE', 'FESTIVAL'],
      ciudades: ['Buenos Aires'],
      rangoPrecio: {
        min: NumberDecimal('20000'),
        max: NumberDecimal('300000'),
        moneda: 'ARS'
      },
      artistas: ['Metallica', 'Iron Maiden', 'Gojira'],
      notificaciones: {
        push: true,
        email: true,
        tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado',
                'NuevoEventoArtistaFavorito', 'DevolucionAprobada']
      },
      fechaActualizacion: ISODate('2026-10-01T12:00:00.000Z')
    }
  },
  { upsert: true }
);

// --- Migración de documentos viejos sin idCompra (volumen ya existente) ---
// Sin esto, el upsert por idCompra crearía un duplicado de la compra ya sembrada.
prefDb.interacciones_eventos.updateOne(
  {
    idCliente: ID_CLIENTE_DEMO,
    idEvento: ID_EVENTO_METALLICA,
    tipo: 'COMPRA',
    idCompra: { $exists: false }
  },
  { $set: { idCompra: 'f0000000-0000-4000-8000-000000000021' } }
);
prefDb.interacciones_eventos.updateOne(
  {
    idCliente: ID_CLIENTE_DEMO,
    idEvento: ID_EVENTO_ROCK_IN_RIO,
    tipo: 'COMPRA',
    idCompra: { $exists: false }
  },
  { $set: { idCompra: 'f0000000-0000-4000-8000-000000000043' } }
);

// --- Seed: historial del cliente demo (compras reales del seed de MySQL) ---
// idCompra = compra.id_compra de infrastructure/mysql/ventas/init.sql (idempotencia).
prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000021' },
  {
    $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000021',
      idCliente: ID_CLIENTE_DEMO,
      idEvento: ID_EVENTO_METALLICA,
      tipo: 'COMPRA',
      nombreEvento: 'Metallica — M72 World Tour',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Metallica', 'Gojira'],
      fechaEvento: ISODate('2027-03-15T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-01T14:05:00.000Z'),
      monto: NumberDecimal('95000.00'),
      cantidadEntradas: 1
    }
  },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000043' },
  {
    $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000043',
      idCliente: ID_CLIENTE_DEMO,
      idEvento: ID_EVENTO_ROCK_IN_RIO,
      tipo: 'COMPRA',
      nombreEvento: 'Rock in Rio — día metal (cancelado)',
      categoria: 'FESTIVAL',
      ciudad: 'Rio de Janeiro',
      artistas: ['Metallica', 'Iron Maiden'],
      fechaEvento: ISODate('2027-09-18T16:00:00.000Z'),
      fechaCompra: ISODate('2026-09-15T10:05:00.000Z'),
      monto: NumberDecimal('75000.00'),
      cantidadEntradas: 1
    }
  },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  {
    idCliente: ID_CLIENTE_DEMO,
    idEvento: ID_EVENTO_UCL,
    tipo: 'VISUALIZACION'
  },
  {
    $set: {
      idCliente: ID_CLIENTE_DEMO,
      idEvento: ID_EVENTO_UCL,
      tipo: 'VISUALIZACION',
      nombreEvento: 'UEFA Champions League Final 2027',
      categoria: 'DEPORTE',
      ciudad: 'Buenos Aires',
      artistas: ['Real Madrid CF', 'Manchester City FC'],
      fechaEvento: ISODate('2027-05-30T21:00:00.000Z'),
      fechaCompra: null,
      monto: null
    }
  },
  { upsert: true }
);

// --- Seed: grafo de co-compra para que "también compraron" tenga señal ---
// Clientes demo sintéticos (solo historial analítico). Compran Metallica y otros eventos.
const coCompras = [
  ['demo-co-1', 'd0000000-0000-4000-8000-000000000004', 'Iron Maiden — The Future Past Tour', 'RECITAL', 'Buenos Aires', ['Iron Maiden', 'Ghost'], '2027-04-10T21:00:00.000Z', '90000.00'],
  ['demo-co-1', ID_EVENTO_METALLICA, 'Metallica — M72 World Tour', 'RECITAL', 'Buenos Aires', ['Metallica', 'Gojira'], '2027-03-15T21:00:00.000Z', '95000.00'],
  ['demo-co-2', 'd0000000-0000-4000-8000-000000000005', 'Rammstein — Stadium Tour', 'RECITAL', 'Buenos Aires', ['Rammstein'], '2027-04-22T21:30:00.000Z', '110000.00'],
  ['demo-co-2', ID_EVENTO_METALLICA, 'Metallica — M72 World Tour', 'RECITAL', 'Buenos Aires', ['Metallica', 'Gojira'], '2027-03-15T21:00:00.000Z', '95000.00'],
  ['demo-co-3', 'd0000000-0000-4000-8000-000000000004', 'Iron Maiden — The Future Past Tour', 'RECITAL', 'Buenos Aires', ['Iron Maiden', 'Ghost'], '2027-04-10T21:00:00.000Z', '90000.00'],
  ['demo-co-3', 'd0000000-0000-4000-8000-00000000000a', 'Slipknot — Here Comes the Pain', 'RECITAL', 'Buenos Aires', ['Slipknot'], '2027-08-20T21:00:00.000Z', '100000.00'],
  ['demo-co-3', ID_EVENTO_METALLICA, 'Metallica — M72 World Tour', 'RECITAL', 'Buenos Aires', ['Metallica', 'Gojira'], '2027-03-15T21:00:00.000Z', '95000.00'],
  ['demo-co-4', 'd0000000-0000-4000-8000-00000000000d', 'Tool — Fear Inoculum Tour', 'RECITAL', 'Buenos Aires', ['Tool'], '2027-05-08T21:00:00.000Z', '105000.00']
];

for (const [cliente, evento, nombre, categoria, ciudad, artistas, fechaEvento, monto] of coCompras) {
  prefDb.interacciones_eventos.updateOne(
    { idCompra: 'seed-' + cliente + '-' + evento },
    {
      $set: {
        idCompra: 'seed-' + cliente + '-' + evento,
        idCliente: 'b0000000-0000-4000-8000-00000000010' + cliente.slice(-1),
        idEvento: evento,
        tipo: 'COMPRA',
        nombreEvento: nombre,
        categoria,
        ciudad,
        artistas,
        fechaEvento: ISODate(fechaEvento),
        fechaCompra: ISODate('2026-10-20T12:00:00.000Z'),
        monto: NumberDecimal(monto),
        cantidadEntradas: 1
      }
    },
    { upsert: true }
  );
}

print('preferencias init OK: colecciones + indices + seed (preferencias, historial y co-compra)');
