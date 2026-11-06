// msvc-preferencias | DB: preferencias
// Colecciones + índices + seed mínimo según docs/diagramas/modelo-mongodb.md
// IDs lógicos alineados a infrastructure/mysql/README.md (usuario demo, eventos seed).
// Corre solo en el primer arranque del volumen (docker-entrypoint-initdb.d).

const dbName = 'preferencias';
const prefDb = db.getSiblingDB(dbName);

// --- Colecciones ---
prefDb.createCollection('preferencias_clientes');
prefDb.createCollection('interacciones_eventos');

// --- Índices (criterios del modelo: idCliente / idEvento) ---
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

// --- Seed mínimo ---
// idCliente demo = usuarios.usuario b000…0001
// idEvento indie  = eventos.evento  d000…0001
const ID_CLIENTE_DEMO = 'b0000000-0000-4000-8000-000000000001';
const ID_EVENTO_INDIE = 'd0000000-0000-4000-8000-000000000001';
const ID_EVENTO_HAMLET = 'd0000000-0000-4000-8000-000000000002';

prefDb.preferencias_clientes.updateOne(
  { idCliente: ID_CLIENTE_DEMO },
  {
    $set: {
      idCliente: ID_CLIENTE_DEMO,
      categorias: ['RECITAL', 'TEATRO'],
      ciudades: ['Tandil'],
      rangoPrecio: {
        min: NumberDecimal('5000'),
        max: NumberDecimal('30000'),
        moneda: 'ARS'
      },
      artistas: ['Indie Local'],
      fechaActualizacion: ISODate('2026-10-01T12:00:00.000Z')
    }
  },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  {
    idCliente: ID_CLIENTE_DEMO,
    idEvento: ID_EVENTO_INDIE,
    tipo: 'COMPRA'
  },
  {
    $set: {
      idCliente: ID_CLIENTE_DEMO,
      idEvento: ID_EVENTO_INDIE,
      tipo: 'COMPRA',
      categoria: 'RECITAL',
      fechaEvento: ISODate('2026-11-15T21:00:00.000Z'),
      fechaCompra: ISODate('2026-10-01T14:05:00.000Z'),
      monto: NumberDecimal('18000.00')
    }
  },
  { upsert: true }
);

prefDb.interacciones_eventos.updateOne(
  {
    idCliente: ID_CLIENTE_DEMO,
    idEvento: ID_EVENTO_HAMLET,
    tipo: 'VISUALIZACION'
  },
  {
    $set: {
      idCliente: ID_CLIENTE_DEMO,
      idEvento: ID_EVENTO_HAMLET,
      tipo: 'VISUALIZACION',
      categoria: 'TEATRO',
      fechaEvento: ISODate('2026-11-20T20:30:00.000Z'),
      fechaCompra: null,
      monto: null
    }
  },
  { upsert: true }
);

print('preferencias init OK: colecciones + indices + seed');
