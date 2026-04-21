package app

import jfx.core.state.ReadOnlyProperty
import jfx.core.state.Property

object I18n {
  enum Locale(val code: String) {
    case De extends Locale("de")
    case En extends Locale("en")
  }

  val localeProperty: Property[Locale] = Property(Locale.De)

  def locale: Locale =
    localeProperty.get

  def locale_=(value: Locale): Unit =
    localeProperty.set(value)

  def toggle(): Unit =
    localeProperty.set(localeProperty.get match {
      case Locale.De => Locale.En
      case Locale.En => Locale.De
    })

  def text(key: Key): ReadOnlyProperty[String] =
    localeProperty.map(locale => translations(locale).getOrElse(key, translations(Locale.De)(key)))

  private val translations: Map[Locale, Map[Key, String]] =
    Map(
      Locale.De -> Map(
        Key.AppTitle -> "Live Documentation",
        Key.Version -> "v2.0.0-alpha",
        Key.SidebarLogo -> "JFX2 API",
        Key.Footer -> "Built with JFX2",
        Key.Copyright -> "Pure Scala.js Architecture.",
        Key.SectionWelcome -> "Willkommen",
        Key.SectionInteraction -> "Interaktion",
        Key.SectionConversation -> "Gespräch",
        Key.SectionArchitecture -> "Architektur",
        Key.SectionKnowledge -> "Wissen",
        Key.NavOverview -> "Entdecken",
        Key.NavOverviewSub -> "Die JFX2 Vision",
        Key.NavButton -> "Aktion",
        Key.NavButtonSub -> "Der Puls der App",
        Key.NavImage -> "Bilder",
        Key.NavImageSub -> "Visuelle Identität",
        Key.NavImageCropper -> "ImageCropper",
        Key.NavImageCropperSub -> "Upload & Zuschnitt",
        Key.NavInput -> "Formulare",
        Key.NavInputSub -> "Natürlicher Dialog",
        Key.NavComboBox -> "ComboBox",
        Key.NavComboBoxSub -> "Elegante Auswahl",
        Key.NavEditor -> "Editor",
        Key.NavEditorSub -> "Lexical Playground",
        Key.NavLayout -> "Struktur",
        Key.NavLayoutSub -> "Raum für Design",
        Key.NavWindow -> "Fenster",
        Key.NavWindowSub -> "Raum für Fokus",
        Key.NavTableView -> "Daten",
        Key.NavTableViewSub -> "Atmen und Fließen",
        Key.NavVirtualList -> "VirtualList",
        Key.NavVirtualListSub -> "Unendliche Weiten",
        Key.NavDomain -> "Domain",
        Key.NavDomainSub -> "Mapping & Reflection",
        Key.ThemeLight -> "Light",
        Key.ThemeDark -> "Dark",
        Key.Language -> "DE/EN"
      ),
      Locale.En -> Map(
        Key.AppTitle -> "Live Documentation",
        Key.Version -> "v2.0.0-alpha",
        Key.SidebarLogo -> "JFX2 API",
        Key.Footer -> "Built with JFX2",
        Key.Copyright -> "Pure Scala.js Architecture.",
        Key.SectionWelcome -> "Welcome",
        Key.SectionInteraction -> "Interaction",
        Key.SectionConversation -> "Conversation",
        Key.SectionArchitecture -> "Architecture",
        Key.SectionKnowledge -> "Knowledge",
        Key.NavOverview -> "Discover",
        Key.NavOverviewSub -> "The JFX2 vision",
        Key.NavButton -> "Actions",
        Key.NavButtonSub -> "The pulse of the app",
        Key.NavImage -> "Images",
        Key.NavImageSub -> "Visual identity",
        Key.NavImageCropper -> "ImageCropper",
        Key.NavImageCropperSub -> "Upload & crop",
        Key.NavInput -> "Forms",
        Key.NavInputSub -> "Natural dialogue",
        Key.NavComboBox -> "ComboBox",
        Key.NavComboBoxSub -> "Elegant selection",
        Key.NavEditor -> "Editor",
        Key.NavEditorSub -> "Lexical playground",
        Key.NavLayout -> "Layout",
        Key.NavLayoutSub -> "Room for design",
        Key.NavWindow -> "Windows",
        Key.NavWindowSub -> "Room for focus",
        Key.NavTableView -> "Data",
        Key.NavTableViewSub -> "Breathing and flowing",
        Key.NavVirtualList -> "VirtualList",
        Key.NavVirtualListSub -> "Endless expanses",
        Key.NavDomain -> "Domain",
        Key.NavDomainSub -> "Mapping & reflection",
        Key.ThemeLight -> "Light",
        Key.ThemeDark -> "Dark",
        Key.Language -> "DE/EN"
      )
    )

  enum Key {
    case AppTitle
    case Version
    case SidebarLogo
    case Footer
    case Copyright
    case SectionWelcome
    case SectionInteraction
    case SectionConversation
    case SectionArchitecture
    case SectionKnowledge
    case NavOverview
    case NavOverviewSub
    case NavButton
    case NavButtonSub
    case NavImage
    case NavImageSub
    case NavImageCropper
    case NavImageCropperSub
    case NavInput
    case NavInputSub
    case NavComboBox
    case NavComboBoxSub
    case NavEditor
    case NavEditorSub
    case NavLayout
    case NavLayoutSub
    case NavWindow
    case NavWindowSub
    case NavTableView
    case NavTableViewSub
    case NavVirtualList
    case NavVirtualListSub
    case NavDomain
    case NavDomainSub
    case ThemeLight
    case ThemeDark
    case Language
  }
}
