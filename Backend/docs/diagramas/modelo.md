# Modelo de datos

Modelo **relacional** (MySQL) de los microservicios transaccionales.
Preferencias / historial de señales → MongoDB: ver [`modelo-mongodb.md`](./modelo-mongodb.md).

En cada entidad, los atributos marcados **FK** son claves foráneas (lógicas o físicas según el msvc dueño).

```mermaid
erDiagram
direction TB
    USUARIO {
    UUID idUsuario PK ""  
    string nombre  ""  
    string apellido  ""  
    string email UK ""  
    string passwordHash  ""  
    string ciudad  ""  
    datetime fechaRegistro  ""  
    boolean activo  ""  
    }


	USUARIO_ROL {
		UUID idUsuario PK,FK ""  
		UUID idRol PK,FK ""  
	}

	ROL {
		UUID idRol PK ""  
		string nombre UK ""  
	}
        ASIENTO {
    UUID id PK
    UUID idSector FK
    string fila
    int numero
    string codigo
}
PRECIO_ASIENTO {
UUID id PK
UUID idEvento FK
UUID idAsiento FK
decimal precio
string moneda
boolean activo
}
LUGAR {
UUID id PK ""  
string nombre  ""  
string direccion  ""  
string ciudad  ""  
int capacidadTotal  ""  
}

	SECTOR {
		UUID id PK ""  
		UUID idLugar FK ""  
		string nombre  ""  
		int capacidad  ""  
		string tipo  "" 
        boolean esNumerado 
	}

	PRECIO_SECTOR {
			UUID id PK ""  
			UUID idEvento FK ""  
			UUID idSector FK ""  
			decimal precio ""  
			string moneda ""  
			boolean activo ""  
		}

		PARTICIPANTE {
			UUID id PK ""
			string nombre ""
			string tipo ""
			string descripcion ""
		}

		EVENTO_PARTICIPANTE {
			UUID idEvento PK,FK ""
			UUID idParticipante PK,FK ""
			string rol ""
			int orden ""
		}

		RESERVA {
		UUID idReserva PK ""  
		UUID idCliente FK ""  
		UUID idEvento FK ""  
		datetime fechaCreacion  ""  
		datetime fechaExpiracion  ""  
		string estado  ""  
		decimal total  ""  
	}

	DETALLE_RESERVA {
		UUID idDetalle PK ""  
		UUID idReserva FK ""  
		UUID idSector FK ""  
		int cantidad  ""  
		decimal precioUnitario  ""  
	}

	COMPRA {
		UUID idCompra PK ""  
		UUID idReserva FK ""  
		UUID idCliente FK ""  
		datetime fechaCompra  ""  
		decimal total  ""  
		string moneda  ""  
		string estado  ""  
	}

	PAGO {
		UUID idPago PK ""  
		UUID idCompra FK ""  
		string proveedor  ""  
		string idTransaccionExterna UK ""  
		decimal monto  ""  
		string estado  ""  
		datetime fechaPago  ""  
	}

	CUPO_SECTOR {
		UUID idCupo PK ""  
		UUID idEvento FK ""  
		UUID idSector FK ""  
		int capacidadTotal  ""  
		int cantidadDisponible  ""  
		int cantidadReservada  ""  
		int cantidadVendida  ""  
	}

	ENTRADA {
		UUID idEntrada PK ""  
		UUID idCompra FK ""  
		UUID idCliente FK ""  
		UUID idEvento FK ""  
		UUID idSector FK ""  
		string codigoQR UK ""  
		string estado  ""  
		datetime fechaEmision  ""  
		datetime fechaValidacion  ""  
	}

	DEVOLUCION {
		UUID idDevolucion PK ""  
		UUID idEntrada FK ""  
		string motivo  ""  
		decimal monto  ""  
		datetime fechaSolicitud  ""  
		string estado  ""  
	}

	VALIDACION_ACCESO {
		UUID idValidacion PK ""  
		UUID idEntrada FK ""  
		UUID idPersonalAcceso FK ""  
		datetime fechaValidacion  ""  
		string resultado  ""  
		string dispositivo  ""  
	}

	NOTIFICACION {
		UUID idNotificacion PK ""  
		UUID idUsuario FK ""  
		UUID idEventoOrigen FK ""  
		UUID idPlantilla FK ""  
		string tipo  ""  
		string canal  ""  
		string asunto  ""  
		string contenido  ""  
		datetime fechaCreacion  ""  
		datetime fechaEnvio  ""  
		string estado  ""  
	}

	INTENTO_ENVIO {
		UUID idIntento PK ""  
		UUID idNotificacion FK ""  
		int numeroIntento  ""  
		datetime fechaIntento  ""  
		string resultado  ""  
		string detalleError  ""  
	}

	PLANTILLA_NOTIFICACION {
		UUID idPlantilla PK ""  
		string tipoEvento  ""  
		string canal  ""  
		string asunto  ""  
		string contenido  ""  
		boolean activa  ""  
	}

	EVENTO {
		UUID id PK ""  
		UUID idOrganizador FK ""  
		string nombre  ""  
		string descripcion  ""  
		string tipo  ""  
		datetime fechaInicio  ""  
		datetime fechaFin  ""  
		string estado  ""  
		UUID idLugar FK ""  
		datetime fechaPublicacion  ""
        boolean esAltaDemanda  
	}

ASIENTO }o--|| SECTOR : pertenece
PRECIO_ASIENTO }o--|| ASIENTO : establece
PRECIO_ASIENTO }o--|| EVENTO : corresponde
USUARIO||--o{USUARIO_ROL:"tiene"
ROL||--o{USUARIO_ROL:"asignado"
LUGAR||--o{SECTOR:"contiene"
LUGAR||--o{EVENTO:"alberga"
USUARIO||--o{EVENTO:"organiza"
EVENTO||--o{PRECIO_SECTOR:"define"
SECTOR||--o{PRECIO_SECTOR:"tarifado"
EVENTO||--o{EVENTO_PARTICIPANTE:"tiene"
PARTICIPANTE||--o{EVENTO_PARTICIPANTE:"en"
USUARIO||--o{RESERVA:"reserva"
EVENTO||--o{RESERVA:"sobre"
RESERVA||--|{DETALLE_RESERVA:"contiene"
SECTOR||--o{DETALLE_RESERVA:"en"
RESERVA||--o|COMPRA:"genera"
USUARIO||--o{COMPRA:"compra"
COMPRA||--o{PAGO:"registra"
EVENTO||--o{CUPO_SECTOR:"cupo"
SECTOR||--o{CUPO_SECTOR:"cupo"
CUPO_SECTOR||--o{DETALLE_RESERVA:"disponibilidad"
COMPRA||--o{ENTRADA:"emite"
USUARIO||--o{ENTRADA:"titular"
EVENTO||--o{ENTRADA:"para"
SECTOR||--o{ENTRADA:"sector"
ENTRADA||--o{DEVOLUCION:"puede_tener"
ENTRADA||--o{VALIDACION_ACCESO:"registra"
USUARIO||--o{VALIDACION_ACCESO:"valida"
USUARIO||--o{NOTIFICACION:"destinatario"
EVENTO||--o{NOTIFICACION:"origen"
NOTIFICACION||--o{INTENTO_ENVIO:"registra"
PLANTILLA_NOTIFICACION||--o{NOTIFICACION:"utiliza"
```
