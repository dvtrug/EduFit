import './App.css'
import { Faq } from './components/landing/Faq'
import { Footer } from './components/landing/Footer'
import { Header } from './components/landing/Header'
import { Hero } from './components/landing/Hero'
import { MatchingFlow } from './components/landing/MatchingFlow'
import { ProgressShowcase } from './components/landing/ProgressShowcase'
import { TrustStrip } from './components/landing/TrustStrip'
import { TutorShortlist } from './components/landing/TutorShortlist'

function App() {
  return (
    <div className="site-shell">
      <Header />
      <main>
        <Hero />
        <TrustStrip />
        <MatchingFlow />
        <TutorShortlist />
        <ProgressShowcase />
        <Faq />
      </main>
      <Footer />
    </div>
  )
}

export default App
