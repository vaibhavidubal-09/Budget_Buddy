export default function AuthField({ id, label, type = 'text', ...inputProps }) {
  return (
    <label className="auth-field" htmlFor={id}>
      <span>{label}</span>
      <input id={id} name={id} type={type} {...inputProps} />
    </label>
  )
}
