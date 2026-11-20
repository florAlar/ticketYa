import { LIMITE_ALTA_DEMANDA } from '../../domain/evento'

/**
 * Confirmación previa a publicar. Con cupo > 5000 es solo un aviso: el evento
 * queda marcado como alta demanda sin preguntar (regla de negocio).
 */
const ConfirmarAltaDemandaModal = ({ cupoTotal = 0, onConfirmar, onCancelar }) => {
  const obligatorio = cupoTotal > LIMITE_ALTA_DEMANDA

  return (
    <div className="alta-demanda-backdrop">
      <div
        className="alta-demanda-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="alta-demanda-titulo"
      >
        <h2 id="alta-demanda-titulo" className="alta-demanda-modal__titulo">
          {obligatorio ? 'Evento de alta demanda' : 'Antes de publicar'}
        </h2>

        {obligatorio ? (
          <p className="alta-demanda-modal__texto">
            Tu evento es un evento de alta demanda
          </p>
        ) : (
          <p className="alta-demanda-modal__texto">
            ¿Es este un evento de alta demanda?
          </p>
        )}

        <p className="alta-demanda-modal__detalle">
          El cupo total es de {cupoTotal.toLocaleString('es-AR')} personas
          {obligatorio
            ? ` (supera el límite de ${LIMITE_ALTA_DEMANDA.toLocaleString('es-AR')}), por lo que la fila virtual se activará automáticamente.`
            : `. Si elegís «Sí», se habilitará la fila virtual para este evento.`}
        </p>

        <div className="alta-demanda-modal__acciones">
          {obligatorio ? (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => onConfirmar(true)}
            >
              Continuar
            </button>
          ) : (
            <>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => onConfirmar(true)}
              >
                Sí
              </button>
              <button
                type="button"
                className="btn"
                onClick={() => onConfirmar(false)}
              >
                No
              </button>
              <button type="button" className="btn" onClick={onCancelar}>
                Cancelar
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default ConfirmarAltaDemandaModal
