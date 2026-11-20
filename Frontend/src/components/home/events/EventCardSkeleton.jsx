import Skeleton from '../../ui/Skeleton'

/** Esqueleto de card mientras carga el catálogo. */
const EventCardSkeleton = () => (
  <div className="event-card event-card--skeleton" aria-hidden>
    <div className="event-card__media">
      <Skeleton width="100%" height="100%" radius="0" />
    </div>
    <div className="event-card__body">
      <Skeleton width="30%" height="0.7rem" />
      <Skeleton width="85%" height="1.05rem" />
      <Skeleton width="55%" height="0.8rem" />
      <Skeleton width="40%" height="1rem" style={{ marginTop: '0.4rem' }} />
    </div>
  </div>
)

export default EventCardSkeleton
