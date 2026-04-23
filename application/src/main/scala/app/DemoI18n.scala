package app

import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.i18n.*

object DemoI18n {
  val German: I18nLocale = I18nLocale("de")
  val English: I18nLocale = I18nLocale.En

  val localeProperty: Property[I18nLocale] =
    Property(English)

  def localeLabel: ReadOnlyProperty[String] =
    localeProperty.map {
      case German => "DE"
      case _ => "EN"
    }

  def toggle(): Unit =
    localeProperty.set(localeProperty.get match {
      case German => English
      case _ => German
    })

  def text(message: RuntimeMessage): ReadOnlyProperty[String] =
    resolver.resolve(message, localeProperty)

  def resolveNow(message: RuntimeMessage): String =
    resolver.resolve(message, localeProperty.get)

  object Messages {
    val deleteDocument: RuntimeMessage =
      i18n"Delete document"

    def invitation(user: String, group: String): RuntimeMessage =
      i18n"User $user invited you to $group"

    val staleRule: RuntimeMessage =
      i18n"The English source is the visible identity of the message"

    val fallbackRule: RuntimeMessage =
      i18n"Missing translations fall back to English"
  }

  private val invitationKey =
    Messages.invitation("Mira", "Architecture").key

  private val year = 2026
  private val locale = "DE"
  private val v = "Ada"

  val catalog: MessageCatalog =
    MessageCatalog(
      de(i18n"JFX2 API", "JFX2 API"),
      de(i18n"Welcome", "Willkommen"),
      de(i18n"Discover", "Entdecken"),
      de(i18n"The JFX2 vision", "Die JFX2 Vision"),
      de(i18n"Interaction", "Interaktion"),
      de(i18n"Actions", "Aktion"),
      de(i18n"The pulse of the app", "Der Puls der App"),
      de(i18n"Images", "Bilder"),
      de(i18n"Visual identity", "Visuelle Identität"),
      de(i18n"ImageCropper", "ImageCropper"),
      de(i18n"Upload & crop", "Upload & Zuschnitt"),
      de(i18n"Conversation", "Gespräch"),
      de(i18n"Forms", "Formulare"),
      de(i18n"Natural dialogue", "Natürlicher Dialog"),
      de(i18n"ComboBox", "ComboBox"),
      de(i18n"Elegant selection", "Elegante Auswahl"),
      de(i18n"Editor", "Editor"),
      de(i18n"Lexical playground", "Lexical Playground"),
      de(i18n"Architecture", "Architektur"),
      de(i18n"Layout", "Struktur"),
      de(i18n"Room for design", "Raum für Design"),
      de(i18n"Windows", "Fenster"),
      de(i18n"Room for focus", "Raum für Fokus"),
      de(i18n"Knowledge", "Wissen"),
      de(i18n"Data", "Daten"),
      de(i18n"Breathing and flowing", "Atmen und Fließen"),
      de(i18n"VirtualList", "VirtualList"),
      de(i18n"Endless expanses", "Unendliche Weiten"),
      de(i18n"Domain", "Domain"),
      de(i18n"Mapping & reflection", "Mapping & Reflection"),
      de(i18n"Built with JFX2", "Built with JFX2"),
      de(i18n"Live Documentation", "Live Documentation"),
      de(i18n"Light", "Light"),
      de(i18n"Dark", "Dark"),
      de(i18n"v2.1.2", "v2.1.2"),
      de(i18n"© $year Anjunar. Pure Scala.js Architecture.", "© {year} Anjunar. Pure Scala.js Architektur."),

      de(i18n"JFX2 Showcase", "JFX2 Showcase"),
      de(i18n"Welcome to JFX2", "Willkommen bei JFX2"),
      de(i18n"Your new home for reactive UIs in Scala.js.", "Dein neues Zuhause für reaktive UIs in Scala.js."),
      de(i18n"Origin story", "Entstehungsgeschichte"),
      de(i18n"After 17 years of looking for clarity, the project started to feel less like a thesis and more like relief.", "Nach 17 Jahren auf der Suche nach Klarheit fühlte sich das Projekt weniger wie eine These und mehr wie Erleichterung an."),
      de(i18n"JFX2 is the answer I wanted after living with frameworks that promised simplicity but quietly handed over control. It chooses explicit lifecycles, honest reactivity, and a DSL that stays readable when the codebase grows.", "JFX2 ist die Antwort, die ich nach Jahren mit Frameworks wollte, die Einfachheit versprachen, aber stillschweigend die Kontrolle abgaben. Es setzt auf explizite Lifecycles, ehrliche Reaktivität und eine DSL, die auch dann lesbar bleibt, wenn die Codebasis wächst."),
      de(i18n"Vision", "Vision"),
      de(i18n"A documentation site that feels like a real workbench.", "Eine Dokumentationsseite, die sich wie eine echte Werkbank anfühlt."),
      de(i18n"The showcase should not just prove that components render. It should show how JFX2 is meant to feel: declarative, server-stable, reactive in the browser, and readable enough that you can still nod to it six months later.", "Die Showcase soll nicht nur beweisen, dass Komponenten rendern. Sie soll zeigen, wie sich JFX2 anfühlen soll: deklarativ, server-stabil, reaktiv im Browser und so lesbar, dass man ihr auch sechs Monate später noch zustimmen kann."),
      de(i18n"SSR", "SSR"),
      de(i18n"Server HTML and client hydration share the same structure.", "Server-HTML und Client-Hydration teilen dieselbe Struktur."),
      de(i18n"DSL", "DSL"),
      de(i18n"Templates stay declarative and free of DOM handwork.", "Templates bleiben deklarativ und frei von DOM-Handarbeit."),
      de(i18n"Live", "Live"),
      de(i18n"Every page shows a usable example instead of a dry API list.", "Jede Seite zeigt ein nutzbares Beispiel statt einer trockenen API-Liste."),
      de(i18n"Message-centered I18n", "Message-zentriertes I18n"),
      de(i18n"The English source lives in Scala code. The catalog attaches multiple languages to exactly that one message.", "Der englische Ursprung steht im Scala-Code. Der Katalog hängt mehrere Sprachen an genau diese eine Message."),
      de(i18n"Locale: $locale", "Sprache: {locale}"),
      de(i18n"Switch locale", "Sprache wechseln"),
      de(i18n"Source", "Quelle"),
      de(i18n"Resolved", "Aufgelöst"),
      de(i18n"Readability first", "Lesbarkeit zuerst"),
      de(i18n"Components are shown so their purpose, state, and placement are immediately clear.", "Komponenten werden so gezeigt, dass Zweck, Zustand und Platzierung sofort klar sind."),
      de(i18n"Hydration in view", "Hydration im Blick"),
      de(i18n"Examples avoid hidden DOM drift and keep virtual containers understandable.", "Beispiele vermeiden versteckte DOM-Abweichungen und halten virtuelle Container verständlich."),
      de(i18n"A growing system", "Ein wachsendes System"),
      de(i18n"New components get room for context, variants, API, and architectural hints.", "Neue Komponenten bekommen Raum für Kontext, Varianten, API und architektonische Hinweise."),
      de(i18n"What you find on the component pages", "Was du auf den Komponentenseiten findest"),
      de(i18n"A short explanation of when the component makes sense.", "Eine kurze Erklärung, wann die Komponente sinnvoll ist."),
      de(i18n"At least one real live state with data or interaction.", "Mindestens ein echter Live-Zustand mit Daten oder Interaktion."),
      de(i18n"Concrete DSL examples that stay close to production code.", "Konkrete DSL-Beispiele, die nah an Produktionscode bleiben."),
      de(i18n"Notes about stability, cursor behavior, SSR, or reactive properties.", "Hinweise zu Stabilität, Cursor-Verhalten, SSR oder reaktiven Properties."),
      de(i18n"Next step", "Nächster Schritt"),
      de(i18n"Pick a component on the left. Each page is now denser and still leaves room for more building blocks without losing the thread.", "Wähle links eine Komponente. Jede Seite ist dichter geworden und lässt trotzdem Raum für weitere Bausteine, ohne den roten Faden zu verlieren."),

      de(i18n"Button", "Button"),
      de(i18n"The pulse of your app.", "Der Puls deiner App."),
      de(i18n"A button is small, but it carries responsibility.", "Ein Button ist klein, aber er trägt Verantwortung."),
      de(i18n"In JFX2 the action stays visible in the template: label, event, and surrounding context sit next to each other. That keeps simple buttons easy to read and leaves room for more complex workflows later.", "In JFX2 bleibt die Aktion im Template sichtbar: Label, Event und Kontext stehen nebeneinander. So bleiben einfache Buttons lesbar und es bleibt Platz für komplexere Abläufe."),
      de(i18n"Standard button", "Standard-Button"),
      de(i18n"A focused click target with direct event binding. Ideal for clear, self-contained actions.", "Ein fokussiertes Klickziel mit direkter Eventbindung. Ideal für klare, abgeschlossene Aktionen."),
      de(i18n"Buttons are the heart of interaction. They are not just click targets; they bring your app to life.", "Buttons sind das Herz der Interaktion. Sie sind nicht nur Klickziele, sie bringen deine App zum Leben."),
      de(i18n"Click me and bring me to life", "Klick mich und bring mich zum Leben"),
      de(i18n"Action group", "Aktionsgruppe"),
      de(i18n"Several buttons may sit close together as long as their intent remains distinguishable.", "Mehrere Buttons dürfen eng zusammenstehen, solange ihre Absicht unterscheidbar bleibt."),
      de(i18n"Save", "Speichern"),
      de(i18n"Check", "Prüfen"),
      de(i18n"Reset", "Zurücksetzen"),
      de(i18n"State", "Zustand"),
      de(i18n"The button says what happens", "Der Button sagt, was passiert"),
      de(i18n"A good label describes the next action, not the technical implementation behind it.", "Ein gutes Label beschreibt die nächste Aktion, nicht die technische Umsetzung dahinter."),
      de(i18n"Event", "Event"),
      de(i18n"onClick stays local", "onClick bleibt lokal"),
      de(i18n"The DSL keeps trigger and reaction visible in the same place.", "Die DSL hält Auslöser und Reaktion am selben Ort sichtbar."),
      de(i18n"Feedback", "Feedback"),
      de(i18n"Actions need a response", "Aktionen brauchen eine Antwort"),
      de(i18n"After the click, the interface should show something visible: a message, status, navigation, or data update.", "Nach dem Klick sollte die Oberfläche etwas Sichtbares zeigen: Meldung, Status, Navigation oder Datenänderung."),
      de(i18n"The simplicity of the DSL", "Die Einfachheit der DSL"),
      de(i18n"The core stays intentionally small: create the button, bind the handler, done.", "Der Kern bleibt absichtlich klein: Button erzeugen, Handler binden, fertig."),

      de(i18n"The elegant choice from a set of possibilities.", "Die elegante Auswahl aus mehreren Möglichkeiten."),
      de(i18n"Selection", "Auswahl"),
      de(i18n"A ComboBox must provide orientation, not just hide options.", "Eine ComboBox muss Orientierung geben, nicht nur Optionen verstecken."),
      de(i18n"The showcase uses real data, custom renderers, and a footer action. That makes it clear how value display, row rendering, and identity work together.", "Die Showcase nutzt echte Daten, eigene Renderer und eine Footer-Aktion. So wird klar, wie Wertanzeige, Zeilenrendering und Identität zusammenspielen."),
      de(i18n"items", "items"),
      de(i18n"The available set remains a clearly passed sequence.", "Die verfügbare Menge bleibt eine klar übergebene Sequenz."),
      de(i18n"converter", "converter"),
      de(i18n"The text value can be derived independently from the object model.", "Der Textwert kann unabhängig vom Objektmodell abgeleitet werden."),
      de(i18n"identityBy", "identityBy"),
      de(i18n"Selection stays stable even when objects are delivered again.", "Die Auswahl bleibt stabil, auch wenn Objekte erneut geliefert werden."),
      de(i18n"Team member selector", "Teammitglied-Auswahl"),
      de(i18n"A realistic renderer with avatar, role, selection state, and footer link.", "Ein realistischer Renderer mit Avatar, Rolle, Auswahlzustand und Footer-Link."),
      de(i18n"Choose the project owner", "Projektverantwortlichen auswählen"),
      de(i18n"check", "check"),
      de(i18n"Team settings", "Team-Einstellungen"),
      de(i18n"Renderer", "Renderer"),
      de(i18n"Row and value may differ", "Zeile und Wert dürfen sich unterscheiden"),
      de(i18n"The dropdown row can be rich while the closed value stays compact.", "Die Dropdown-Zeile kann reich sein, während der geschlossene Wert kompakt bleibt."),
      de(i18n"Cursor", "Cursor"),
      de(i18n"Stable identity protects selection", "Stabile Identität schützt die Auswahl"),
      de(i18n"identityBy describes when an entry remains the same domain entry.", "identityBy beschreibt, wann ein Eintrag derselbe Domaineintrag bleibt."),
      de(i18n"Readability", "Lesbarkeit"),
      de(i18n"Configuration stays inside the block", "Konfiguration bleibt im Block"),
      de(i18n"Placeholder, data, heights, and renderers live together in one component.", "Placeholder, Daten, Höhen und Renderer leben gemeinsam in einer Komponente."),
      de(i18n"Usage", "Nutzung"),
      de(i18n"The important decisions stay right inside the comboBox block.", "Die wichtigen Entscheidungen bleiben direkt im comboBox-Block."),

      de(i18n"Forms & input", "Formulare & Eingaben"),
      de(i18n"The flowing dialog with your users.", "Der fließende Dialog mit deinen Nutzern."),
      de(i18n"Form flow", "Formularfluss"),
      de(i18n"Data entry should feel like a structured conversation.", "Dateneingabe sollte sich wie ein strukturiertes Gespräch anfühlen."),
      de(i18n"InputContainer, Input, and Properties separate presentation, value, and validation while staying readable together in the template. You can see immediately which fields exist and how they react.", "InputContainer, Input und Properties trennen Darstellung, Wert und Validierung, bleiben aber gemeinsam im Template lesbar. Man sieht sofort, welche Felder existieren und wie sie reagieren."),
      de(i18n"Property", "Property"),
      de(i18n"The visible value can flow reactively.", "Der sichtbare Wert kann reaktiv fließen."),
      de(i18n"Validator", "Validator"),
      de(i18n"Error rules stay close to the field.", "Fehlerregeln bleiben nah am Feld."),
      de(i18n"Form", "Form"),
      de(i18n"Several controls share one context for submit and reset.", "Mehrere Controls teilen einen Kontext für Submit und Reset."),
      de(i18n"Simple text input", "Einfaches Texteingabefeld"),
      de(i18n"Standalone inputs are ideal for search fields, short filters, or small dialogs without a full form context.", "Standalone-Inputs sind ideal für Suchfelder, kurze Filter oder kleine Dialoge ohne vollständigen Form-Kontext."),
      de(i18n"Enter name...", "Namen eingeben..."),
      de(i18n"Input: $v", "Eingabe: {v}"),
      de(i18n"Standalone usage", "Standalone-Nutzung"),
      de(i18n"The property remains explicit. That makes it immediately clear where the value flows.", "Die Property bleibt explizit. Dadurch ist sofort klar, wohin der Wert fließt."),
      de(i18n"Domain-bound form", "Domain-gebundenes Formular"),
      de(i18n"Forms can bind directly to domain objects. Reflection connects field names with properties.", "Formulare können direkt an Domänenobjekte gebunden werden. Reflection verbindet Feldnamen mit Properties."),
      de(i18n"Full name", "Vollständiger Name"),
      de(i18n"E-mail", "E-Mail"),
      de(i18n"Address (SubForm)", "Adresse (SubForm)"),
      de(i18n"Street", "Straße"),
      de(i18n"City", "Stadt"),
      de(i18n"Validate", "Validieren"),
      de(i18n"The form contains errors.", "Das Formular enthält Fehler."),
      de(i18n"Everything is fine.", "Alles ist in Ordnung."),
      de(i18n"Street must not be empty", "Straße darf nicht leer sein"),
      de(i18n"City must not be empty", "Stadt darf nicht leer sein"),
      de(i18n"Name must be at least 3 characters long", "Name muss mindestens 3 Zeichen lang sein"),
      de(i18n"Current user: ${I18n.named("name", "Max")} (${I18n.named("email", "max@example.com")}) lives in ${I18n.named("city", "Musterstadt")}", "Aktueller Nutzer: {name} ({email}) wohnt in {city}"),
      de(i18n"Domain binding", "Domain-Binding"),
      de(i18n"Passing an object to form lets inputs find the matching properties automatically.", "Durch die Übergabe eines Objekts an form finden Inputs automatisch die passenden Properties."),
      de(i18n"Binding", "Binding"),
      de(i18n"Value flow stays visible", "Wertfluss bleibt sichtbar"),
      de(i18n"The field writes into a Property or registers itself in the form context.", "Das Feld schreibt in eine Property oder registriert sich im Form-Kontext."),
      de(i18n"Validation", "Validierung"),
      de(i18n"Errors belong to the field", "Fehler gehören ans Feld"),
      de(i18n"Rules sit where a reader expects them, not in a distant submit method.", "Regeln sitzen dort, wo ein Leser sie erwartet, nicht in einer entfernten Submit-Methode."),
      de(i18n"Submit", "Submit"),
      de(i18n"Forms collect behavior", "Formulare sammeln Verhalten"),
      de(i18n"The form context makes validation, clearing, and reading visible as shared operations.", "Der Form-Kontext macht Validieren, Leeren und Lesen als gemeinsame Operationen sichtbar."),
      de(i18n"Form and DI usage", "Form- und DI-Nutzung"),
      de(i18n"For larger forms, the form context is the calmer structure.", "Für größere Formulare ist der Form-Kontext die ruhigere Struktur."),

      de(i18n"TableView", "TableView"),
      de(i18n"Large data sets that breathe and flow.", "Datenmengen, die atmen und fließen."),
      de(i18n"Data view", "Datenansicht"),
      de(i18n"A table is good when it makes large data feel calm.", "Eine Tabelle ist erst gut, wenn sie große Daten ruhig macht."),
      de(i18n"The JFX2 TableView combines reactive virtualization with crawlable SSR. In the browser users scroll smoothly, while crawlers can reach the full data set through real HTML links.", "Die JFX2 TableView verbindet reaktive Virtualisierung mit crawlbarem SSR. Im Browser scrollen Nutzer flüssig, während Crawler die gesamte Datenmenge über echte HTML-Links erreichen."),
      de(i18n"ListProperty", "ListProperty"),
      de(i18n"Local data mutates reactively.", "Lokale Daten mutieren reaktiv."),
      de(i18n"RemoteList", "RemoteList"),
      de(i18n"Remote data loads by page or by range.", "Remote-Daten laden seiten- oder bereichsweise nach."),
      de(i18n"Virtual rows", "Virtuelle Zeilen"),
      de(i18n"Only visible rows are physically rendered.", "Nur sichtbare Zeilen werden physisch gerendert."),
      de(i18n"Local TableView", "Lokale TableView"),
      de(i18n"The control itself: items, columns, row height, selection, and reactive ListProperty mutations.", "Die Control selbst: Items, Columns, RowHeight, Selection und reaktive ListProperty-Mutationen."),
      de(i18n"No row selected yet.", "Noch keine Zeile ausgewählt."),
      de(i18n"Title", "Titel"),
      de(i18n"Author", "Autor"),
      de(i18n"Year", "Jahr"),
      de(i18n"Selected: ${I18n.named("title", "Der Hobbit")} by ${I18n.named("author", "J. R. R. Tolkien")}", "Ausgewählt: {title} von {author}"),
      de(i18n"Select first row", "Erste Zeile auswählen"),
      de(i18n"Add row", "Zeile hinzufügen"),
      de(i18n"TableView DSL", "TableView DSL"),
      de(i18n"This is the actual control usage, independent of whether the data is local or remote.", "Das ist die eigentliche Control-Nutzung, unabhängig davon, ob die Daten lokal oder remote kommen."),
      de(i18n"TableView control logic", "TableView-Control-Logik"),
      de(i18n"The important state lives in the TableView itself, not in the route.", "Die wichtigsten Zustände leben in der TableView selbst, nicht in der Route."),
      de(i18n"Holds the current ListProperty. When it changes, observers are rewired and visible rows are recalculated.", "Hält die aktuelle ListProperty. Beim Wechsel werden Observer neu verdrahtet und sichtbare Rows neu berechnet."),
      de(i18n"Contains only the row slots that must be visible for scrollTop, viewportHeight, and overscan.", "Enthält nur die Row-Slots, die für scrollTop, viewportHeight und Overscan sichtbar sein müssen."),
      de(i18n"Distributes column widths from prefWidth and viewportWidth for header and body.", "Verteilt Spaltenbreiten aus prefWidth und viewportWidth stabil auf Header und Body."),
      de(i18n"Provides loading, error, sorting, and range state. The TableView triggers lazy or range loading from the visible area.", "Liefert Loading-, Error-, Sorting- und Range-State. Die TableView triggert Lazy- oder Range-Loading aus dem sichtbaren Bereich."),
      de(i18n"In SSR, the visible area is determined through offset and limit and a real More link is created.", "Im SSR wird der sichtbare Bereich über offset und limit bestimmt und ein echter More-Link erzeugt."),
      de(i18n"Row clicks set selectedIndexProperty; selectedItemProperty is updated from it.", "Zeilenklicks setzen selectedIndexProperty; daraus wird selectedItemProperty aktualisiert."),
      de(i18n"Virtualization internals", "Virtualisierung intern"),
      de(i18n"The control does not render the whole list, but calculates the visible range from scroll and viewport state.", "Die Control rendert nicht die ganze Liste, sondern berechnet aus Scroll- und Viewport-State den sichtbaren Bereich."),
      de(i18n"1000", "1000"),
      de(i18n"Remotely loaded book rows in the example.", "Remote geladene Buchzeilen im Beispiel."),
      de(i18n"50", "50"),
      de(i18n"Rows per initial remote page.", "Zeilen pro initialer Remote-Page."),
      de(i18n"3", "3"),
      de(i18n"Sortable columns with stable sort keys.", "Sortierbare Spalten mit stabilen Sort-Keys."),
      de(i18n"Crawlable book catalog", "Crawlbarer Bücherkatalog"),
      de(i18n"Remote data, virtual rows, sortable columns, and crawlable navigation in one example.", "Remote-Daten, virtuelle Zeilen, sortierbare Spalten und crawlbare Navigation in einem Beispiel."),
      de(i18n"Remote", "Remote"),
      de(i18n"The query is explicit", "Die Query ist explizit"),
      de(i18n"Index, limit, and sorting together form the repeatable state of the data request.", "Index, Limit und Sorting bilden zusammen den wiederholbaren Zustand der Datenanfrage."),
      de(i18n"Virtual rows need stable DOM paths", "Virtuelle Zeilen brauchen stabile DOM-Pfade"),
      de(i18n"SSR, hydration, and ForEach must refer to the same physical nodes.", "SSR, Hydration und ForEach müssen dieselben physischen Knoten meinen."),
      de(i18n"SEO", "SEO"),
      de(i18n"Crawlable remains a product option", "Crawlable bleibt eine fachliche Option"),
      de(i18n"The table can offer real links without giving up browser performance.", "Die Tabelle kann echte Links anbieten, ohne Browser-Performance aufzugeben."),
      de(i18n"Sorting and lazy loading belong to the data source. The TableView only asks for those capabilities.", "Sortierung und Lazy Loading gehören zur Datenquelle. Die TableView fragt diese Fähigkeiten nur ab."),
      de(i18n"Async route usage", "Async-Route-Nutzung"),
      de(i18n"The route is only the SSR shell: it loads the first page before rendering so SSR and hydration share the same initial state.", "Die Route ist nur die SSR-Hülle: Sie lädt die erste Page vor dem Rendern, damit SSR und Hydration denselben Anfangszustand teilen."),

      de(Messages.deleteDocument, "Dokument löschen"),
      de(RuntimeMessage(invitationKey, Messages.invitation("Mira", "Architecture").args), "Benutzer {user} hat dich zu {group} eingeladen"),
      de(Messages.staleRule, "Der englische Ursprung ist die sichtbare Identität der Message"),
      de(Messages.fallbackRule, "Fehlende Übersetzungen fallen auf Englisch zurück"),

      de(i18n"JFX2", "JFX2"),
      de(i18n"Components", "Komponenten"),
      de(i18n"Showcase surface", "Showcase-Fläche"),
      de(i18n"Navigation leads from the left, while the right side keeps room for the active component and its explanation.", "Die Navigation führt von links, während rechts Raum für die aktive Komponente und ihre Erklärung bleibt."),
      de(i18n"Live demo", "Live-Demo"),
      de(i18n"API", "API"),
      de(i18n"Notes", "Notizen"),
      de(i18n"H1", "H1"),
      de(i18n"H2", "H2"),
      de(i18n"V1", "V1"),
      de(i18n"V2", "V2"),
      de(i18n"Left", "Links"),
      de(i18n"Right", "Rechts"),
      de(i18n"I was clicked! The magic begins.", "Ich wurde geklickt! Die Magie beginnt."),
      de(i18n"Saved.", "Gespeichert."),
      de(i18n"Checked.", "Geprüft."),
      de(i18n"Search member...", "Mitglied suchen..."),
      de(i18n"Choose member...", "Mitglied wählen..."),
      de(i18n"records in the showcase", "Datensätze in der Showcase"),
      de(i18n"variable row heights for real layout tension", "Variable Zeilenhöhen für echte Layout-Spannung"),
      de(i18n"Only what the user needs right now is rendered", "Nur das wird gerendert, was der Nutzer gerade braucht"),
      de(i18n"Record #${I18n.named("i", 0)}", "Datensatz #$i"),
      de(i18n"Loading...", "Lädt..."),
      de(i18n"Viewport", "Viewport"),
      de(i18n"Roundtrip OK: ${I18n.named("name", "")} from ${I18n.named("city", "")}", "Rundlauf erfolgreich: {name} aus {city}"),

      de(i18n"Image Cropper", "Bild zuschneiden"),
      de(i18n"Upload images, crop them, and store them as Media.", "Bilder hochladen, zuschneiden und als Media speichern."),
      de(i18n"The cropper is its own form control.", "Der Cropper ist ein eigenes Formular-Control."),
      de(i18n"ImageCropper bundles file selection, crop dialog, thumbnail creation, and Media binding. That keeps the Image component simple and gives the interactive upload its own playground.", "ImageCropper bündelt Dateiauswahl, Crop-Dialog, Thumbnail-Erzeugung und Media-Binding. Dadurch bleibt die Image-Komponente schlicht und der interaktive Upload bekommt seinen eigenen Spielplatz."),
      de(i18n"Crop profile image", "Profilbild zuschneiden"),
      de(i18n"Square cropping with a live preview of the generated thumbnail.", "Quadratischer Zuschnitt mit Live-Vorschau des erzeugten Thumbnails."),
      de(i18n"Choose profile image", "Profilbild auswählen"),
      de(i18n"Cropped profile image", "Zugeschnittenes Profilbild"),
      de(i18n"Result", "Ergebnis"),
      de(i18n"No image selected yet.", "Noch kein Bild ausgewählt."),
      de(i18n"Wide aspect ratio", "Breites Seitenverhältnis"),
      de(i18n"The same cropper can be configured directly for header or banner images.", "Der gleiche Cropper kann direkt für Header- oder Bannerbilder konfiguriert werden."),
      de(i18n"Choose banner image", "Bannerbild auswählen"),
      de(i18n"Crop banner image", "Banner zuschneiden"),
      de(i18n"Readonly state", "Readonly-Zustand"),
      de(i18n"Like the other controls, the cropper follows the shared editable DSL.", "Wie die anderen Controls folgt auch der Cropper der gemeinsamen editable-DSL."),
      de(i18n"Readonly: upload and cropping are disabled", "Readonly: Upload und Zuschnitt sind deaktiviert"),

      de(i18n"Images & graphics", "Bilder & Grafiken"),
      de(i18n"A picture says more than a thousand lines of code.", "Ein Bild sagt mehr als tausend Zeilen Code."),
      de(i18n"Visual presence", "Visuelle Präsenz"),
      de(i18n"Images give your application depth and identity.", "Bilder geben deiner Anwendung Tiefe und Identität."),
      de(i18n"Simple inclusion of an image with source and alt text.", "Ein einfaches Bild mit Quelle und Alt-Text."),
      de(i18n"A cute cat", "Eine niedliche Katze"),
      de(i18n"Dynamic image", "Dynamisches Bild"),
      de(i18n"The source can be bound to a property to swap images at runtime.", "Die Quelle kann an eine Property gebunden werden, um Bilder zur Laufzeit zu wechseln."),
      de(i18n"Cat 1", "Katze 1"),
      de(i18n"Cat 2", "Katze 2"),

      de(i18n"The writing surface activates on the client", "Die Schreibfläche aktiviert sich im Client"),
      de(i18n"Short external property update. The editor adopts it after hydration.", "Kurzes externes Property-Update. Der Editor übernimmt es nach der Hydration."),
      de(i18n"This value was set outside the editor and is synchronized to both instances.", "Dieser Wert wurde außerhalb des Editors gesetzt und wird in beide Instanzen synchronisiert."),
      de(i18n"Shell stays stable", "Die Hülle bleibt stabil"),
      de(i18n"Toolbar does not flicker", "Die Toolbar flackert nicht"),
      de(i18n"Text stays visible", "Text bleibt sichtbar"),
      de(i18n"External values", "Externe Werte"),
      de(i18n"Fallback and client version use the same jfx-editor shell with toolbar area and surface frame.", "Fallback und Client-Version verwenden dieselbe jfx-editor-Hülle mit Toolbar-Bereich und Surface-Frame."),
      de(i18n"Readonly renders the toolbar hidden on the server so hydration does not have to surprise the structure.", "Readonly rendert die Toolbar auf dem Server verborgen, damit die Hydration die Struktur nicht überraschend verändert."),
      de(i18n"Plain text or Lexical JSON is extracted into preview text during SSR and then adopted by Lexical after hydration.", "Klartext oder Lexical-JSON werden während SSR als Vorschautext extrahiert und nach der Hydration von Lexical übernommen."),
      de(i18n"valueProperty updates are synchronized back into Lexical after mount via parseEditorState.", "valueProperty-Updates werden nach dem Mount über parseEditorState zurück in Lexical synchronisiert."),
      de(i18n"Base", "Basis"),
      de(i18n"Paragraphs and headings through a dropdown.", "Absätze und Überschriften über ein Dropdown."),
      de(i18n"Bullet and numbered list commands.", "Bullet- und Numbered-List-Befehle."),
      de(i18n"Dialog service for inserting links.", "Dialog-Service zum Einfügen von Links."),
      de(i18n"Image dialog as an editor plugin.", "Bild-Dialog als Editor-Plugin."),
      de(i18n"Table nodes and toolbar actions.", "Tabellenknoten und Toolbar-Aktionen."),
      de(i18n"Code block plugin as a specialized node.", "Code-Block-Plugin als spezialisierter Knoten."),

      de(i18n"Open window", "Fenster öffnen"),
      de(i18n"A room for thoughts", "Ein Raum für Gedanken"),
      de(i18n"There is room for your ideas here.", "Hier ist Raum für deine Ideen."),
      de(i18n"The note in the window was confirmed.", "Die Notiz im Fenster wurde bestätigt."),
      de(i18n"The structure is now sound.", "Die Struktur ist jetzt solide."),
      de(i18n"Warning: the form may be hardening.", "Warnung: Die Form könnte aushärten."),
      de(i18n"A crack in the foundation was discovered.", "Es wurde ein Riss im Fundament entdeckt."),
      de(i18n"Confirm note", "Notiz bestätigen"),
      de(i18n"The viewport is the quiet center that carries windows, notifications, and overlays. It brings order to the chaos and gives it a stage.", "Der Viewport ist das ruhige Zentrum, das Fenster, Benachrichtigungen und Overlays trägt. Er bringt Ordnung ins Chaos und gibt ihm eine Bühne."),

      de(i18n"registered classes", "registrierte Klassen"),
      de(i18n"reflected properties", "reflektierte Properties"),
      de(i18n"validator annotations", "Validator-Annotationen"),
      de(i18n"Not yet run.", "Noch nicht ausgeführt."),
      de(i18n"Not yet validated.", "Noch nicht validiert."),
      de(i18n"All clean: the annotations produced no validation errors.", "Alles sauber: Die Annotationen haben keine Validierungsfehler erzeugt."),
      de(i18n"Roundtrip failed.", "Roundtrip fehlgeschlagen."),
      de(i18n"Validation errors present.", "Validierungsfehler vorhanden."),
      de(i18n"Mapper", "Mapper")
    )

  private val resolver =
    I18nResolver(catalog)

  private def de(message: RuntimeMessage, translation: String): CatalogEntry =
    I18n.entry(message.key).translations(
      German -> translation
    )
}
