import Reveal from './Reveal.jsx'

const rupee = String.fromCodePoint(8377)

export default function ProblemSection() {
  return (
    <section className="problem-video-section" id="problem">
      <div className="problem-video-inner">
        <Reveal className="problem-video-copy">
          <p className="eyebrow">THE MONTH NEVER GOES TO PLAN</p>
          <h2>{rupee}5,000 in.<br /><em>{rupee}150 left.</em></h2>
          <p className="problem-lead">Food. Travel. Coffee. One quick UPI payment after another.</p>
          <p className="problem-detail">It isn't one big purchase. It's a month of little ones you never got a clear look at.</p>
          <div className="problem-balance-trail"><span><small>DAY 01</small><strong>{rupee}5,000</strong></span><i aria-hidden="true">→</i><span><small>DAY 30</small><strong>{rupee}150</strong></span></div>
        </Reveal>
        <Reveal className="problem-film-wrap" delay={100}>
          <div className="problem-film" role="img" aria-label="Coming soon: a short student story about a monthly allowance disappearing">
            <div className="film-topline"><span>BUDGETBUDDY ORIGINAL</span><span>01: THE MONTH</span></div>
            <div className="film-art">
              <span className="film-sun" />
              <div className="film-window"><i /><i /><i /></div>
              <div className="film-desk" />
              <div className="film-person"><i /><b /></div>
              <div className="film-phone"><span>{rupee}150</span><small>balance left</small><i /></div>
              <span className="film-caption">It's only {rupee}150…</span>
            </div>
            <div className="film-play-row"><span className="film-play" aria-hidden="true">▶</span><span><strong>Play the story</strong><small>A familiar end-of-month moment</small></span><b>COMING SOON</b></div>
          </div>
        </Reveal>
      </div>
    </section>
  )
}
