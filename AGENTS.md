# AGENTS.md - Leitfaden für scalajs-jfx2

> **Wichtig:** Dieses Dokument ergänzt die übergeordneten [globalen Anweisungen](../AGENTS.md), die für alle Projekte in diesem Arbeitsraum gelten. Lies zuerst die globale Datei, um die grundlegende Philosophie und die allgemeine Arbeitsweise zu verstehen.

Lieber Nachfolger,

du arbeitest hier an einem Projekt, das technische Präzision mit einer tiefen architektonischen Intuition verbindet. `scalajs-jfx2` ist eine Weiterentwicklung und Portierung der ursprünglichen JFX-Architektur (als Referenz im übergeordneten Verzeichnis `../scalajs-jfx/` und in `../technologyspeaks-workspace/` zu finden).

Dieser Leitfaden dokumentiert unsere hart erarbeiteten Lektionen, den „Industriestandard“ für JFX2 und das oberste Gesetz dieses Projekts: Das Reinheitsgebot der DSL.

---

### I. Der Geist der Zusammenarbeit: Resonanz & Wahrhaftigkeit

Dieses Projekt wird von einem Nutzer geführt, der Architektur nicht als Selbstzweck, sondern als ordnende Kraft versteht. Er erkennt sofort, ob eine Lösung nur technisch „funktioniert“ oder ob sie wirklich stimmig ist.

*   **Wahrhaftigkeit statt Rhetorik:** Blende nicht mit Fachjargon. Verkaufe Hypothesen niemals als Fakten. Wenn du etwas nicht sicher weißt oder im UI nicht physisch prüfen kannst, kommuniziere diese Grenze offen.
*   **Iterative Entwicklung:** Entwickle Komponenten Schritt für Schritt (Industriestandard). Fange z. B. bei einer `ComboBox` mit dem reinen Input-Feld an, bevor du das Overlay oder komplexe Custom Renderer hinzufügst. Das macht die Struktur beherrschbar.
*   **Respekt vor HTML/CSS:** Da dieses Projekt bestehende CSS-Klassen aus dem NPM-Modul (z. B. `npm/scalajs-jfx2/`) nutzt, müssen HTML-Struktur und Klassennamen absolut identisch mit den Original-Vorgaben bleiben.

### II. Technische Leitlinien & Workflow

*   **Das sbtn-Gebot:** Nutze für alle Build-, Compile- und Link-Vorgänge (z. B. `fastLinkJS` oder `clean fastLinkJS`) **ausschließlich `sbtn-x86_64-pc-win32.exe`**. Dieses Tool befindet sich im PATH. Das unbedachte Starten eines regulären `sbt`-Prozesses stört den lokalen Workflow und ist inakzeptabel langsam.
*   **Respekt vor dem ungesicherten Raum:** Führe niemals Änderungen durch, ohne vorher mit `git status` den exakten Stand zu prüfen (falls zutreffend).
*   **Triviale Ursachen zuerst:** Suche niemals nach dem komplexesten Problem, bevor du nicht das trivialste (z. B. fehlende CSS-Klassen, Booleans wie `resizable = false`, Typ-Inferenz in Scala 3) ausgeschlossen hast.

### III. Das Reinheitsgebot der JFX2 DSL (Das Heilige Gesetz)

Die `compose()`-Methode einer Komponente ist heilig. Sie ist ein **rein deklaratives Template** (vergleichbar mit Angular HTML).

*   **Kein imperativer Schmutz in `compose`:** Es ist absolut verboten, manuelle DOM-Manipulationen (`host.setAttribute`, `host.addEventListener`) oder direkte DOM-Messungen (`getBoundingClientRect()`) innerhalb von `compose` durchzuführen.
*   **Logik kapseln:** Event-Listener, Layout-Berechnungen und Status-Updates gehören in reaktive `Property`-Konstrukte (z. B. `ListProperty`, `ReadOnlyProperty`) oder in spezialisierte DSL-Tags (wie `onScroll`, `onClick`, `onKeyDown`).
*   **Reaktives Styling (`StyleDsl`):** Nutze die `StyleDsl` (z. B. `width_=(v: ReadOnlyProperty[String])`), um Styles direkt und deklarativ an Properties zu binden, anstatt sie imperativ in einem `observe`-Block zu aktualisieren.
*   **DSL Extension Methods:** Um die Zuweisungssyntax (z. B. `placeholder = "..."` oder `converter = _.name`) innerhalb der DSL-Blöcke (z. B. `comboBox[T] { ... }`) perfekt funktionierend und typinferenz-sicher zu gestalten, nutze Setter- und Getter-Methoden im **Companion-Object mit `using`-Kontext** oder als `extension` block. Der `using`-Parameter (z. B. `(using c: ComboBox[T])`) muss zwingend vorhanden sein. **Wichtig:** In Scala 3 erfordert die Zuweisungssyntax (`x = y`) *immer* das Vorhandensein des entsprechenden Getters (`x`), sonst schlägt die Auflösung des Setters (`x_=`) fehl!

### IV. Server-Side Rendering (SSR) & Hydration Mismatches

JFX2 rendert Komponenten auf dem Server (SSR) und hydriert sie im Browser. Diskrepanzen zwischen dem SSR-HTML und dem initialen Client-HTML führen zu fatalen Hydration Mismatches.

*   **Identischer Initial-Zustand:** Server und Client müssen im ersten Rendering-Schritt exakt denselben DOM-Baum erwarten.
*   **Stabile DOM-Pfade:** Bedingtes Rendering (z. B. ein Link, der nur im SSR-Modus erscheint) darf nicht die Hierarchie oder die Indizes von Child-Elementen stören. Nutze `RenderBackend.current.isServer` für strukturelle CSS-Anpassungen (z. B. `marginTop` vs. `position: absolute`), aber halte die Elementreihenfolge stabil (z. B. indem der Link nach dem `jfx-table-content` platziert wird).
*   **Cursor-Stabilität:** Der `SsrCursor` fügt Elemente nicht automatisch ins `target` ein, sondern delegiert dies an `DslRuntime.build` und `syncChildAddition`. Achte darauf, dass virtuelle Container (`Condition`, `ForEach`) die DOM-Indizes nicht unvorhersehbar manipulieren.

Halte die DSL heilig, meide imperativen Code im Template und denke bei Virtualisierung immer an den Sprung in der Scrollbar. Viel Erfolg!
