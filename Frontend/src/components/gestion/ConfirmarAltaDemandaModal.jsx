import { HiOutlineLightningBolt } from 'react-icons/hi'
import { LIMITE_ALTA_DEMANDA } from '../../domain/evento'
import Modal from '../ui/Modal'
import Button from '../ui/Button'

/**
 * Confirmación previa a publicar. Con cupo > 5000 es solo un aviso: el evento
 * queda marcado como alta demanda sin preguntar (regla de negocio).
 */
const ConfirmarAltaDemandaModal = ({ cupoTotal = 0, onConfirmar, onCancelar }) => {
  const obligatorio = cupoTotal > LIMITE_ALTA_DEMANDA
  const cupo = cupoTotal.toLocaleString('es-AR')

  return (
    <Modal
      icon={HiOutlineLightningBolt}
      title={obligatorio ? 'Tu evento es de alta demanda' : '¿Es un evento de alta demanda?'}
      description={
        obligatorio
          ? `El cupo total es de ${cupo} personas (supera el límite de ${LIMITE_ALTA_DEMANDA.toLocaleString('es-AR')}), por lo que la fila virtual se activará automáticamente.`
          : `El cupo total es de ${cupo} personas. Si elegís «Sí», se habilita la fila virtual: los clientes entran en cola y compran por tandas, en orden de llegada.`
      }
      onClose={onCancelar}
      dismissible={!obligatorio}
      actions={
        obligatorio ? (
          <Button onClick={() => onConfirmar(true)}>Continuar y publicar</Button>
        ) : (
          <>
            <Button variant="ghost" onClick={onCancelar}>
              Cancelar
            </Button>
            <Button variant="secondary" onClick={() => onConfirmar(false)}>
              No
            </Button>
            <Button onClick={() => onConfirmar(true)}>Sí, activar fila</Button>
          </>
        )
      }
    />
  )
}

export default ConfirmarAltaDemandaModal
