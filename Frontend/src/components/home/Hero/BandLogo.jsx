/**
 * Logotipo de la banda para el hero. Usa el logo oficial si está cargado en
 * `assets/img/hero/logos/<id>.svg|png|webp`; si no, un logotipo tipográfico con el
 * carácter de cada banda (estilos `.band-logo--<id>` en Hero.css).
 */
const BandLogo = ({ banda }) => {
  if (banda.logoUrl) {
    return (
      <img
        className="band-logo band-logo--img"
        src={banda.logoUrl}
        alt={banda.nombre}
        style={banda.escala ? { "--logo-scale": banda.escala } : undefined}
      />
    )
  }
  const lineas = banda.lineas || [banda.nombre]
  return (
    <span className={`band-logo band-logo--${banda.id}`} role="img" aria-label={banda.nombre}>
      {lineas.map((l) => (
        <span key={l} className="band-logo__line" aria-hidden>
          {banda.id === 'rhcp' && l === 'Red Hot' && <i className="band-logo__star">✱</i>}
          {banda.id === 'rammstein' ? (
            <>
              <b className="band-logo__flip">R</b>
              {l.slice(1)}
            </>
          ) : (
            l
          )}
        </span>
      ))}
    </span>
  )
}

export default BandLogo
