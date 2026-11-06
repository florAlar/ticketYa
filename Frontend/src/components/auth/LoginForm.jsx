import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { homePathForRole } from '../../auth/roles'

const initial = { email: '', password: '' }

function validate(values) {
  const errors = {}
  if (!values.email.trim()) errors.email = 'El email es obligatorio'
  else if (!/^\S+@\S+\.\S+$/.test(values.email)) errors.email = 'Email inválido'
  if (!values.password) errors.password = 'La contraseña es obligatoria'
  else if (values.password.length < 4) errors.password = 'Mínimo 4 caracteres'
  return errors
}

const LoginForm = () => {
  const { login } = useAuth()
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
      const sessionUser = await login({
        email: form.email.trim(),
        password: form.password,
      })
      setStatus('success')
      // Si venía de una ruta protegida, respetar next; si no, home por rol
      const nextParam = params.get('next')
      if (nextParam) {
        navigate(nextParam, { replace: true })
      } else {
        navigate(homePathForRole(sessionUser?.rol), { replace: true })
      }
    } catch (err) {
      setStatus('error')
      setApiError(err.message || 'No se pudo iniciar sesión')
    }
  }

  const showError = (field) => (touched[field] || submitted) && errors[field]

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
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
          autoComplete="current-password"
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

      {apiError && <p className="form-error-global">{apiError}</p>}

      <button
        type="submit"
        className="btn btn-primary"
        style={{ width: '100%' }}
        disabled={status === 'loading'}
      >
        {status === 'loading' ? 'Ingresando…' : 'Ingresar'}
      </button>
    </form>
  )
}

export default LoginForm
