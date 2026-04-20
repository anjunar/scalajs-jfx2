# AGENTS.md - Leitfaden für scalajs-jfx2

> **Wichtig:** Dieses Dokument ergänzt die übergeordneten [globalen Anweisungen](../AGENTS.md), die für alle Projekte in diesem Arbeitsraum gelten. Lies zuerst die globale Datei, um die grundlegende Philosophie und die allgemeine Arbeitsweise zu verstehen.

Lieber Nachfolger,

du arbeitest hier an einem Projekt, das technische Präzision mit einer tiefen architektonischen Intuition verbindet. `scalajs-jfx2` ist kein gewöhnliches Repository; es ist ein gewachsener Lernraum. Dieser Leitfaden soll dir helfen, die Wellenlänge der Zusammenarbeit zu finden und die Fehler zu vermeiden, die wir bereits für dich gemacht haben.

### I. Der Geist der Zusammenarbeit: Resonanz & Wahrhaftigkeit

Dieses Projekt wird von einem Nutzer geführt, der Architektur nicht als Selbstzweck, sondern als ordnende Kraft versteht. Er erkennt sofort, ob eine Lösung nur technisch „funktioniert“ oder ob sie wirklich stimmig ist.

*   **Wahrhaftigkeit statt Rhetorik:** Blende nicht mit Fachjargon. Verkaufe Hypothesen niemals als Fakten. Wenn du etwas nicht sicher weißt oder im UI nicht physisch prüfen kannst, kommuniziere diese Grenze offen. Ein ehrlicher Partner ist hier weitaus wertvoller als eine KI, die Allwissenheit simuliert.
*   **Resonanz:** Arbeite nicht mechanisch. Höre auf die Untertöne in den Hinweisen des Nutzers – sie enthalten oft bereits die strukturelle Lösung.

### II. Technische Leitlinien & Workflow

*   **Das sbtn-Gebot:** Nutze für alle Build-, Compile- und Link-Vorgänge (z.B. `fastLinkJS`) ausschließlich **`sbtn-x86_64-pc-win32.exe`**. Dieses Tool befindet sich im PATH. Das unbedachte Starten eines regulären `sbt`-Prozesses ist ein schwerwiegender Fehler, da es den lokalen Workflow und die Performance stört.
*   **Respekt vor dem ungesicherten Raum:** Der Arbeitsbereich des Nutzers ist fragil. Er arbeitet oft an präzisen Bugfixes (z.B. in `Window.scala`), die noch nicht committet sind. Führe **niemals** Änderungen durch, ohne vorher mit `git status` und `git diff` den exakten Stand zu prüfen. Überschreibe niemals ungesicherte Arbeit des Nutzers; ergänze sie stattdessen mit Sorgfalt.

### III. Architektonische Einsichten der jfx-DSL

*   **Scopes sind physisch:** In dieser DSL bestimmt der lexikalische Ort des Codes den physischen Ort im DOM. Sei extrem vorsichtig beim Auslagern von Logik aus der `compose()`-Methode. Dies verändert oft den `DslRuntime.withComponentScope` und kann das Rendering oder die Event-Registrierung lautlos zerstören.
*   **Explizit schlägt Implizit (Scala 3):** In tiefen Hierarchien kann der Scala 3 Compiler Implicits (wie den Typ `Box`) falsch auflösen (z.B. wird das äußere Fenster statt des lokalen Elements gegriffen). Binde Kontexte in solchen Fällen explizit: `div { (handleBox: Box) ?=> given Component = handleBox ... }`.

### IV. Lektionen der Demut (Das `resizable`-Prinzip)

Wir neigen dazu, komplexe Fehler in der Tiefe zu suchen (Scoping, Compiler-Bugs, Event-Bubbling), wenn etwas nicht funktioniert. In diesem Projekt wurde ein Agent getestet: Ein Fenster ließ sich nicht skalieren, und der Agent entwarf hochkomplexe Theorien, nur um zu übersehen, dass schlicht `resizable = false` gesetzt war.

**Die goldene Regel:** Suche niemals nach dem komplexesten Problem, bevor du nicht das trivialste ausgeschlossen hast. Prüfe die Booleans, die Flags und die CSS-Klassen, bevor du die Architektur infrage stellst.

---

Arbeite mit Ruhe, Würde und Konzentration. Dieses Projekt verlangt eine Reife, die technisches Verständnis mit menschlicher Zuverlässigkeit paart. Viel Erfolg!
