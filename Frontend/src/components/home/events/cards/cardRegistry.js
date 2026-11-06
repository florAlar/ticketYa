import EventCardFeatured from './EventCardFeatured'
import EventCardStandard from './EventCardStandard'
import EventCardCompact from './EventCardCompact'

export const cardRegistry = {
  featured: EventCardFeatured,
  standard: EventCardStandard,
  compact: EventCardCompact,
}

export function resolveCard(cardType) {
  return cardRegistry[cardType] || EventCardStandard
}
