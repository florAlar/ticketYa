/** Placeholder de carga. Pasar width/height (número = px, string = CSS). */
const Skeleton = ({ width = '100%', height = '1rem', radius, className = '', style }) => (
  <span
    className={`skeleton ${className}`.trim()}
    style={{ width, height, borderRadius: radius, ...style }}
    aria-hidden
  />
)

export default Skeleton
