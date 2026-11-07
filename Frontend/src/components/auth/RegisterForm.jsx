import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'

const initial = {
  nombre: '',
  email: '',
  password: '',
  confirmPassword: '',
}

function validate(values) {
  const errors = {}
  if (!values.nombre.trim()) errors.nombre = 'El nombre es obligatorio'
  else if (values.nombre.trim().length < 2) errors.nombre = 'Mínimo 2 caracteres'

  if (!values.email.trim()) errors.email = 'El email es obligatorio'
  else if (!/^\S+@\S+\.\S+$/.test(values.email)) errors.email = 'Email inválido'

  if (!values.password) errors.password = 'La contraseña es obligatoria'
  else if (values.password.length < 4) errors.password = 'Mínimo 4 caracteres'

  if (!values.confirmPassword) errors.confirmPassword = 'Confirmá la contraseña'
  else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = 'Las contraseñas no coinciden'
  }
  return errors
}

const RegisterForm = () => {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [form, setForm] = useState(initial)
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [submitted, setSubmitted] = useState(false)
  const [status, setStatus] = useState('idle')
  const [apiError, setApiError] = useState('')

  const handleChange = (e) => {
    const { id, value } = e.target
    const next = { ...form, [id]: value }
    setForm(next)
    if (touched[id] || submitted) setErrors(validate(next))
  }

  const handleBlur = (e) => {
    const { id } = e.target
    setTouched((prev) => ({ ...prev, [id]: true }))
    setErrors(validate(form))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitted(true)
    setApiError('')
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    try {
      setStatus('loading')
      await register({
        nombre: form.nombre.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      setStatus('success')
      const next = params.get('next') || '/'
      navigate(next, { replace: true })
    } catch (err) {
      setStatus('error')
      setApiError(err.message || 'No se pudo crear la cuenta')
    }
  }

  const showError = (field) => (touched[field] || submitted) && errors[field]

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <div className="form-group">
        <label htmlFor="nombre">Nombre</label>
        <input
          id="nombre"
          name="nombre"
          autoComplete="name"
          className={`form-input ${showError('nombre') ? 'is-error' : ''}`}
          value={form.nombre}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={status === 'loading'}
        />
        {showError('nombre') && (
          <span className="form-error">{errors.nombre}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          className={`form-input ${showError('email') ? 'is-error' : ''}`}
          value={form.email}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={status === 'loading'}
        />
        {showError('email') && <span className="form-error">{errors.email}</span>}
      </div>

      <div className="form-group">
        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="new-password"
          className={`form-input ${showError('password') ? 'is-error' : ''}`}
          value={form.password}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={status === 'loading'}
        />
        {showError('password') && (
          <span className="form-error">{errors.password}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="confirmPassword">Confirmar contraseña</label>
        <input
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          className={`form-input ${showError('confirmPassword') ? 'is-error' : ''}`}
          value={form.confirmPassword}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={status === 'loading'}
        />
        {showError('confirmPassword') && (
          <span className="form-error">{errors.confirmPassword}</span>
        )}
      </div>

      {apiError && <p className="form-error-global">{apiError}</p>}

      <p className="auth-box__roles-note" style={{ marginBottom: '0.75rem' }}>
        Esta cuenta será de <strong>cliente</strong> (comprar entradas). No
        incluye panel de organizador ni admin.
      </p>

      <button
        type="submit"
        className="btn btn-primary"
        style={{ width: '100%' }}
        disabled={status === 'loading'}
      >
        {status === 'loading' ? 'Creando cuenta…' : 'Crear cuenta cliente'}
      </button>
    </form>
  )
}

export default RegisterForm
