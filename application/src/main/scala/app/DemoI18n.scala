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
      de(i18n"v2.0.0", "v2.0.0"),
      de(i18n"© $year Anjunar. Pure Scala.js Architecture.", "© {year} Anjunar. Pure Scala.js Architektur."),
      de(Messages.deleteDocument, "Dokument löschen"),
      de(RuntimeMessage(invitationKey, Messages.invitation("Mira", "Architecture").args), "Benutzer {user} hat dich zu {group} eingeladen"),
      de(Messages.staleRule, "Der englische Ursprung ist die sichtbare Identität der Message")
    )

  private val resolver =
    I18nResolver(catalog)

  private def de(message: RuntimeMessage, translation: String): CatalogEntry =
    I18n.entry(message.key).translations(
      German -> translation
    )
}
