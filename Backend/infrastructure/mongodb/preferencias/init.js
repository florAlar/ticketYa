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


// ============================================================================
// Plataforma en funcionamiento: preferencias e historial de compras de los clientes seed.
// idCompra = compra.id_compra de infrastructure/mysql/ventas/init.sql (idempotencia).
// ============================================================================
prefDb.preferencias_clientes.updateOne(
  { idCliente: 'b0000000-0000-4000-8000-000000000005' },
  { $set: {
      idCliente: 'b0000000-0000-4000-8000-000000000005',
      categorias: ['RECITAL'],
      ciudades: ['Buenos Aires'],
      rangoPrecio: { min: NumberDecimal('60000'), max: NumberDecimal('250000'), moneda: 'ARS' },
      artistas: ['Rammstein','Judas Priest','Sepultura'],
      notificaciones: { push: true, email: true, tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado', 'NuevoEventoArtistaFavorito', 'DevolucionAprobada'] },
      fechaActualizacion: ISODate('2026-11-01T12:00:00.000Z')
  } },
  { upsert: true }
);

prefDb.preferencias_clientes.updateOne(
  { idCliente: 'b0000000-0000-4000-8000-000000000006' },
  { $set: {
      idCliente: 'b0000000-0000-4000-8000-000000000006',
      categorias: ['RECITAL','DEPORTE','FESTIVAL'],
      ciudades: ['Córdoba','Buenos Aires'],
      rangoPrecio: { min: NumberDecimal('40000'), max: NumberDecimal('200000'), moneda: 'ARS' },
      artistas: ['Pantera','Lamb of God'],
      notificaciones: { push: true, email: true, tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado', 'NuevoEventoArtistaFavorito', 'DevolucionAprobada'] },
      fechaActualizacion: ISODate('2026-11-01T12:00:00.000Z')
  } },
  { upsert: true }
);

prefDb.preferencias_clientes.updateOne(
  { idCliente: 'b0000000-0000-4000-8000-000000000007' },
  { $set: {
      idCliente: 'b0000000-0000-4000-8000-000000000007',
      categorias: ['RECITAL','DEPORTE'],
      ciudades: ['Rosario','Buenos Aires'],
      rangoPrecio: { min: NumberDecimal('50000'), max: NumberDecimal('220000'), moneda: 'ARS' },
      artistas: ['Iron Maiden','Red Bull Rampage'],
      notificaciones: { push: true, email: true, tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado', 'NuevoEventoArtistaFavorito', 'DevolucionAprobada'] },
      fechaActualizacion: ISODate('2026-11-01T12:00:00.000Z')
  } },
  { upsert: true }
);

prefDb.preferencias_clientes.updateOne(
  { idCliente: 'b0000000-0000-4000-8000-000000000008' },
  { $set: {
      idCliente: 'b0000000-0000-4000-8000-000000000008',
      categorias: ['DEPORTE'],
      ciudades: ['Buenos Aires'],
      rangoPrecio: { min: NumberDecimal('40000'), max: NumberDecimal('300000'), moneda: 'ARS' },
      artistas: ['Boca Juniors','Real Madrid CF'],
      notificaciones: { push: true, email: true, tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado', 'NuevoEventoArtistaFavorito', 'DevolucionAprobada'] },
      fechaActualizacion: ISODate('2026-11-01T12:00:00.000Z')
  } },
  { upsert: true }
);

prefDb.preferencias_clientes.updateOne(
  { idCliente: 'b0000000-0000-4000-8000-000000000009' },
  { $set: {
      idCliente: 'b0000000-0000-4000-8000-000000000009',
      categorias: ['RECITAL','DEPORTE'],
      ciudades: ['Mendoza'],
      rangoPrecio: { min: NumberDecimal('50000'), max: NumberDecimal('160000'), moneda: 'ARS' },
      artistas: ['Mastodon','Slipknot','X Games'],
      notificaciones: { push: true, email: true, tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado', 'NuevoEventoArtistaFavorito', 'DevolucionAprobada'] },
      fechaActualizacion: ISODate('2026-11-01T12:00:00.000Z')
  } },
  { upsert: true }
);

prefDb.preferencias_clientes.updateOne(
  { idCliente: 'b0000000-0000-4000-8000-000000000010' },
  { $set: {
      idCliente: 'b0000000-0000-4000-8000-000000000010',
      categorias: ['RECITAL','DEPORTE'],
      ciudades: ['La Plata','Buenos Aires'],
      rangoPrecio: { min: NumberDecimal('40000'), max: NumberDecimal('180000'), moneda: 'ARS' },
      artistas: ['Pantera','WRC','Supercross'],
      notificaciones: { push: true, email: true, tipos: ['CompraConfirmada', 'EventoCancelado', 'EventoSuspendido', 'EventoModificado', 'NuevoEventoArtistaFavorito', 'DevolucionAprobada'] },
      fechaActualizacion: ISODate('2026-11-01T12:00:00.000Z')
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000301' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000301',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-000000000021',
      tipo: 'COMPRA',
      nombreEvento: 'Supercross Indoor — Gran Final',
      categoria: 'DEPORTE',
      ciudad: 'Buenos Aires',
      artistas: ['Supercross'],
      fechaEvento: ISODate('2027-02-20T20:00:00.000Z'),
      fechaCompra: ISODate('2026-11-10T18:25:00.000Z'),
      monto: NumberDecimal('60000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000302' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000302',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-000000000016',
      tipo: 'COMPRA',
      nombreEvento: 'Sepultura — Roots Bloody Roots Night',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Sepultura'],
      fechaEvento: ISODate('2027-01-23T21:00:00.000Z'),
      fechaCompra: ISODate('2026-11-12T11:10:00.000Z'),
      monto: NumberDecimal('260000.00'),
      cantidadEntradas: 2
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000303' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000303',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-00000000001c',
      tipo: 'COMPRA',
      nombreEvento: 'Pantera — Reunion Show',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Pantera'],
      fechaEvento: ISODate('2027-01-09T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-05T16:45:00.000Z'),
      monto: NumberDecimal('120000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000304' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000304',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-00000000001f',
      tipo: 'COMPRA',
      nombreEvento: 'Lamb of God — Omens World Tour',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Lamb of God'],
      fechaEvento: ISODate('2027-02-06T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-20T09:20:00.000Z'),
      monto: NumberDecimal('180000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000305' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000305',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-000000000012',
      tipo: 'COMPRA',
      nombreEvento: 'Boca Juniors vs River Plate — Superclásico',
      categoria: 'DEPORTE',
      ciudad: 'Buenos Aires',
      artistas: ['Boca Juniors','River Plate'],
      fechaEvento: ISODate('2027-08-29T17:00:00.000Z'),
      fechaCompra: ISODate('2026-11-17T13:35:00.000Z'),
      monto: NumberDecimal('140000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000306' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000306',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-000000000018',
      tipo: 'COMPRA',
      nombreEvento: 'Judas Priest — Invincible Shield Tour',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Judas Priest'],
      fechaEvento: ISODate('2027-02-14T21:00:00.000Z'),
      fechaCompra: ISODate('2026-11-10T20:15:00.000Z'),
      monto: NumberDecimal('210000.00'),
      cantidadEntradas: 2
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000307' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000307',
      idCliente: 'b0000000-0000-4000-8000-000000000005',
      idEvento: 'd0000000-0000-4000-8000-000000000005',
      tipo: 'COMPRA',
      nombreEvento: 'Rammstein — Stadium Tour',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Rammstein'],
      fechaEvento: ISODate('2027-04-22T21:30:00.000Z'),
      fechaCompra: ISODate('2026-11-19T22:15:00.000Z'),
      monto: NumberDecimal('220000.00'),
      cantidadEntradas: 2
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000308' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000308',
      idCliente: 'b0000000-0000-4000-8000-000000000005',
      idEvento: 'd0000000-0000-4000-8000-00000000001e',
      tipo: 'COMPRA',
      nombreEvento: 'WRC — Rally de Argentina',
      categoria: 'DEPORTE',
      ciudad: 'Santiago del Estero',
      artistas: ['WRC'],
      fechaEvento: ISODate('2027-05-15T09:00:00.000Z'),
      fechaCompra: ISODate('2026-11-14T12:05:00.000Z'),
      monto: NumberDecimal('85000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000309' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000309',
      idCliente: 'b0000000-0000-4000-8000-000000000006',
      idEvento: 'd0000000-0000-4000-8000-00000000001b',
      tipo: 'COMPRA',
      nombreEvento: 'Motocross de las Naciones — Gran Final',
      categoria: 'DEPORTE',
      ciudad: 'Santiago del Estero',
      artistas: ['Motocross de las Naciones'],
      fechaEvento: ISODate('2027-01-16T13:00:00.000Z'),
      fechaCompra: ISODate('2026-09-30T17:05:00.000Z'),
      monto: NumberDecimal('45000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-00000000030a' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-00000000030a',
      idCliente: 'b0000000-0000-4000-8000-000000000006',
      idEvento: 'd0000000-0000-4000-8000-000000000003',
      tipo: 'COMPRA',
      nombreEvento: 'Rock in Rio — día metal (cancelado)',
      categoria: 'FESTIVAL',
      ciudad: 'Rio de Janeiro',
      artistas: [],
      fechaEvento: ISODate('2027-09-18T16:00:00.000Z'),
      fechaCompra: ISODate('2026-09-20T11:05:00.000Z'),
      monto: NumberDecimal('75000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-00000000030b' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-00000000030b',
      idCliente: 'b0000000-0000-4000-8000-000000000007',
      idEvento: 'd0000000-0000-4000-8000-000000000004',
      tipo: 'COMPRA',
      nombreEvento: 'Iron Maiden — The Future Past Tour',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Iron Maiden'],
      fechaEvento: ISODate('2027-04-10T21:00:00.000Z'),
      fechaCompra: ISODate('2026-11-16T19:50:00.000Z'),
      monto: NumberDecimal('320000.00'),
      cantidadEntradas: 2
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-00000000030c' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-00000000030c',
      idCliente: 'b0000000-0000-4000-8000-000000000007',
      idEvento: 'd0000000-0000-4000-8000-00000000001d',
      tipo: 'COMPRA',
      nombreEvento: 'Red Bull Rampage — Freeride Extremo',
      categoria: 'DEPORTE',
      ciudad: 'Santiago del Estero',
      artistas: ['Red Bull Rampage'],
      fechaEvento: ISODate('2027-03-06T11:00:00.000Z'),
      fechaCompra: ISODate('2026-11-20T10:35:00.000Z'),
      monto: NumberDecimal('150000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-00000000030d' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-00000000030d',
      idCliente: 'b0000000-0000-4000-8000-000000000008',
      idEvento: 'd0000000-0000-4000-8000-000000000002',
      tipo: 'COMPRA',
      nombreEvento: 'UEFA Champions League Final 2027',
      categoria: 'DEPORTE',
      ciudad: 'Buenos Aires',
      artistas: ['Real Madrid CF','Manchester City FC'],
      fechaEvento: ISODate('2027-05-30T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-12T21:05:00.000Z'),
      monto: NumberDecimal('280000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-00000000030f' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-00000000030f',
      idCliente: 'b0000000-0000-4000-8000-000000000009',
      idEvento: 'd0000000-0000-4000-8000-000000000017',
      tipo: 'COMPRA',
      nombreEvento: 'Mastodon — Hushed and Grim Live',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Mastodon','Gojira'],
      fechaEvento: ISODate('2027-01-30T21:00:00.000Z'),
      fechaCompra: ISODate('2026-11-21T09:55:00.000Z'),
      monto: NumberDecimal('78000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000311' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000311',
      idCliente: 'b0000000-0000-4000-8000-000000000010',
      idEvento: 'd0000000-0000-4000-8000-000000000020',
      tipo: 'COMPRA',
      nombreEvento: 'X Games — Buenos Aires',
      categoria: 'DEPORTE',
      ciudad: 'Santiago del Estero',
      artistas: ['X Games'],
      fechaEvento: ISODate('2027-02-27T12:00:00.000Z'),
      fechaCompra: ISODate('2026-11-18T14:05:00.000Z'),
      monto: NumberDecimal('55000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000312' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000312',
      idCliente: 'b0000000-0000-4000-8000-000000000010',
      idEvento: 'd0000000-0000-4000-8000-00000000001c',
      tipo: 'COMPRA',
      nombreEvento: 'Pantera — Reunion Show',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Pantera'],
      fechaEvento: ISODate('2027-01-09T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-02T12:05:00.000Z'),
      monto: NumberDecimal('120000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000313' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000313',
      idCliente: 'b0000000-0000-4000-8000-000000000010',
      idEvento: 'd0000000-0000-4000-8000-00000000001c',
      tipo: 'COMPRA',
      nombreEvento: 'Pantera — Reunion Show',
      categoria: 'RECITAL',
      ciudad: 'Buenos Aires',
      artistas: ['Pantera'],
      fechaEvento: ISODate('2027-01-09T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-02T12:10:00.000Z'),
      monto: NumberDecimal('210000.00'),
      cantidadEntradas: 1
  } },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  { idCompra: 'f0000000-0000-4000-8000-000000000315' },
  { $set: {
      idCompra: 'f0000000-0000-4000-8000-000000000315',
      idCliente: 'b0000000-0000-4000-8000-000000000006',
      idEvento: 'd0000000-0000-4000-8000-000000000012',
      tipo: 'COMPRA',
      nombreEvento: 'Boca Juniors vs River Plate — Superclásico',
      categoria: 'DEPORTE',
      ciudad: 'Buenos Aires',
      artistas: ['Boca Juniors','River Plate'],
      fechaEvento: ISODate('2027-08-29T17:00:00.000Z'),
      fechaCompra: ISODate('2026-11-13T21:05:00.000Z'),
      monto: NumberDecimal('120000.00'),
      cantidadEntradas: 2
  } },
  { upsert: true }
);
