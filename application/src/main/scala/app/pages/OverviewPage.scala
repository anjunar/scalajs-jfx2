package app.pages

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object OverviewPage {
  def render() = {
    showcasePage("Willkommen bei JFX2", "Deinem neuen Zuhause für reaktive UIs in Scala.js.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Vision",
          "Eine Dokumentation, die wie eine echte Werkbank wirkt.",
          "Der Showcase soll nicht nur beweisen, dass Komponenten rendern. Er soll zeigen, wie JFX2 gedacht ist: deklarativ, serverseitig stabil, im Browser reaktiv und so lesbar, dass man nach sechs Monaten noch freundlich nickt."
        )

        metricStrip(
          "SSR" -> "Server-HTML und Client-Hydration teilen dieselbe Struktur.",
          "DSL" -> "Templates bleiben deklarativ und frei von DOM-Handarbeit.",
          "Live" -> "Jede Seite zeigt ein nutzbares Beispiel statt trockener API-Listen."
        )

        insightGrid(
          ("01", "Lesbarkeit zuerst", "Komponenten werden so gezeigt, dass ihr Zweck, ihr Zustand und ihre Einbettung sofort erfassbar sind."),
          ("02", "Hydration im Blick", "Beispiele vermeiden versteckte DOM-Abweichungen und halten virtuelle Container nachvollziehbar."),
          ("03", "Wachsendes System", "Neue Komponenten bekommen Platz für Kontext, Varianten, API und Architekturhinweise.")
        )

        patternList(
          "Was du in den Komponentenseiten findest",
          "Eine kurze Einordnung, wann die Komponente sinnvoll ist.",
          "Mindestens ein echter Live-Zustand mit Daten oder Interaktion.",
          "Konkrete DSL-Beispiele, die nah am Produktionscode bleiben.",
          "Hinweise zu Stabilität, Cursor, SSR oder reaktiven Properties."
        )

        noteBlock(
          "Nächster Schritt",
          "Wähle links eine Komponente. Jede Seite ist jetzt dichter aufgebaut und lässt Platz für weitere Bausteine, ohne den roten Faden zu verlieren."
        )
      }
    }
  }
}
