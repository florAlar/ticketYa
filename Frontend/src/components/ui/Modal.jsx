import * as Dialog from '@radix-ui/react-dialog'

/**
 * Modal accesible (Radix Dialog: foco atrapado, Escape, aria). El contenido y
 * las acciones los define el llamador. `onClose` se invoca con Escape o clic afuera
 * (salvo `dismissible={false}`).
 */
const Modal = ({
  open = true,
  onClose,
  title,
  description,
  icon: Icon,
  dismissible = true,
  children,
  actions,
}) => (
  <Dialog.Root open={open} onOpenChange={(v) => !v && onClose?.()}>
    <Dialog.Portal>
      <Dialog.Overlay className="modal-overlay" />
      <Dialog.Content
        className="modal-content"
        onInteractOutside={(e) => !dismissible && e.preventDefault()}
        onEscapeKeyDown={(e) => !dismissible && e.preventDefault()}
      >
        {Icon && (
          <div className="modal-icon" aria-hidden>
            <Icon />
          </div>
        )}
        <Dialog.Title className="modal-title">{title}</Dialog.Title>
        {description ? (
          <Dialog.Description className="modal-desc">{description}</Dialog.Description>
        ) : (
          <Dialog.Description className="modal-desc" hidden />
        )}
        {children && <div className="modal-body">{children}</div>}
        {actions && <div className="modal-actions">{actions}</div>}
      </Dialog.Content>
    </Dialog.Portal>
  </Dialog.Root>
)

export default Modal
