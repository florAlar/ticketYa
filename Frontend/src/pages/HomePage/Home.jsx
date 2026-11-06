import Hero from '../../components/home/Hero/Hero'
import EventSection from '../../components/home/events/EventSection'
import { homeSections } from './homeSections'
import '../../components/home/events/events.css'

const Home = () => {
  return (
    <div className="home-page">
      <Hero />
      {homeSections.map((section) => (
        <EventSection
          key={section.id}
          title={section.title}
          filter={section.filter}
          layout={section.layout}
          card={section.card}
          linkToAll={section.linkToAll}
        />
      ))}
    </div>
  )
}

export default Home
