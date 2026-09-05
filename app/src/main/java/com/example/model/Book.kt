package com.example.model

import com.example.R

data class Book(
  val id: Int,
  val title: String,
  val subtitle: String,
  val author: String = "Julian Vance",
  val year: String,
  val editionNumber: String,
  val coverRes: Int,
  val chapterTitle: String,
  val readingProgress: Float,
  val currentPage: Int,
  val totalPages: Int,
  val excerpt: String,
  val fullText: List<String>,
)

object BookRepository {
  val curatedBooks = listOf(
    Book(
      id = 1,
      title = "The Architecture of Silence",
      subtitle = "Monastic Spacing & Solitary Thought",
      author = "Julian Vance",
      year = "2026",
      editionNumber = "ARCHIVE NO. 042 / 100",
      coverRes = R.drawable.img_book_1,
      chapterTitle = "Chapter III • The Acoustics of Stillness",
      readingProgress = 0.42f,
      currentPage = 48,
      totalPages = 312,
      excerpt = "To construct a room for silence is not to subtract sound, but to tune the resonance of what remains.",
      fullText = listOf(
        "To construct a room for silence is not merely to subtract sound, but to tune the subtle resonance of what remains. In the cloistered arcades of Thoronet and the vaulted corridors of Sénanque, stone does not absorb speech; it receives it as a transient vibration, smoothing harshness into an echo that returns only as ambient presence.",
        "When an author steps into such enclosures, the cadence of thought slows to match the thermal mass of granite. We are accustomed in contemporary life to an architecture of velocity—glass surfaces that reflect only haste, partitions that transmit anxiety.",
        "Here, conversely, the wall possesses gravity. A single window, cut deep into limestone with a 45-degree splay, gathers the morning light and diffuses it across whitewashed lime plaster with an evenness that renders artificial illumination unnecessary. In this sanctuary, the page becomes the floorplan of an inner chamber.",
        "Consider the margin of the printed page. It is not wasted paper; it is the physical moat that shields the text from the noise of the surrounding world. Just as a Japanese teahouse requires an entry crawl-space (nijiriguchi) to humble the visitor, a worthy book demands an expanse of blank cream paper before the first sentence may begin.",
        "We read, ultimately, not to consume data, but to occupy a space designed by an architect of language. The sentences must carry structural integrity: verbs acting as load-bearing columns, subordinate clauses as cantilevered balconies overlooking quiet courtyards of reflection.",
        "In this proprietary edition, the text is sealed against casual dissemination. It exists only for the deliberate eye, held within an authenticated viewport where words retain their weight, unclouded by the frantic mechanisms of the digital crowd."
      )
    ),
    Book(
      id = 2,
      title = "Solitude & Form",
      subtitle = "Studies in Void, Weight, and Proportion",
      author = "Julian Vance",
      year = "2025",
      editionNumber = "ARCHIVE NO. 018 / 100",
      coverRes = R.drawable.img_book_2,
      chapterTitle = "Chapter I • The Geometry of Solitude",
      readingProgress = 0.18f,
      currentPage = 14,
      totalPages = 284,
      excerpt = "Form is what happens when solitude is granted sufficient time to harden into outline.",
      fullText = listOf(
        "Form is what happens when solitude is granted sufficient time to harden into outline. Left to itself, human contemplation is diffuse, mist-like, drifting across memories and unfinished gestures without settling into durable shape.",
        "Only under the deliberate pressure of seclusion does the material crystallize. The sculptor knows that marble does not yield to haste; every chisel blow is a subtraction that reveals an inevitable contour already dormant within the quarry.",
        "In our era of hyper-connectivity, solitude is often treated as an error code—a malfunction to be corrected by immediate notification. Yet throughout intellectual history, every profound contribution to philosophy and literature arose from sustained, uninterrupted seclusion.",
        "The mind needs borders just as a painting requires a frame. Without limits, imagination dissipates into ambient noise. By establishing rigorous artistic constraints—restricting our shelf to three works, purifying our canvas of extraneous buttons—we return dignity to the reading act.",
        "The reader who opens this volume enters a contractual stillness. No alerts will arrive; no algorithms will measure your scroll speed to optimize engagement. There is only the charcoal ligature upon cream fiber, and the silent covenant between author and witness."
      )
    ),
    Book(
      id = 3,
      title = "Ephemeral Light",
      subtitle = "Notes on Radiance, Dawn, and Impermanence",
      author = "Julian Vance",
      year = "2024",
      editionNumber = "ARCHIVE NO. 087 / 100",
      coverRes = R.drawable.img_book_3,
      chapterTitle = "Chapter V • Translucence and Memory",
      readingProgress = 0.75f,
      currentPage = 147,
      totalPages = 196,
      excerpt = "The most enduring ideas are those written with the lightness of dust floating across a sunbeam.",
      fullText = listOf(
        "The most enduring ideas are those written with the lightness of dust floating across a sunbeam. They do not demand assent with theatrical rhetoric; they illuminate quietly, allowing the reader to discover the truth as if remembering a long-forgotten dream.",
        "At dawn, before the city awakens, light enters sideways. It strikes the spine of a book shelved three decades ago, gilding the gold foil embossing with a momentary flash of gold. For five minutes, that forgotten object becomes the brightest thing in the library.",
        "Literature is that slant light. It illuminates what was already present in the reader's spirit, giving names to sensations that were previously mute. In designing these digital manuscripts, we sought the tactile tranquility of that early morning study.",
        "The subtle grain of paper, the restraint of charcoal typography, and the absence of intrusive interface machinery all conspire toward one end: that nothing shall stand between the radiance of the sentence and the stillness of the reader's mind."
      )
    )
  )
}
