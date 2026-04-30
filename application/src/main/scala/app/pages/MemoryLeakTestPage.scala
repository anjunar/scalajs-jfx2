package app.pages

import app.DemoI18n
import app.components.Showcase.*
import jfx.action.Button.button
import jfx.control.VirtualListView
import jfx.control.VirtualListView.*
import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Editor.*
import jfx.form.editor.plugins.{basePlugin, codePlugin, headingPlugin, imagePlugin, linkPlugin, listPlugin, tablePlugin}
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.statement.Condition.*
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval, setTimeout}

object MemoryLeakTestPage {

  final case class EditorRow(id: Int, title: String, value: js.Any)

  def render(): Unit = {
    val rows = new ListProperty[EditorRow]()
    rows.setAll((1 to 1000).map { index =>
      EditorRow(
        index,
        s"Editor #$index",
        lexicalState(s"Memory leak test document $index. This row intentionally mounts a Lexical editor inside the VirtualList.")
      )
    })

    val mountedProperty = Property(true)
    val statusProperty = Property("Mounted. Ready for a baseline heap snapshot.")
    val heapProperty = Property(heapLabel("Heap"))
    var listView: VirtualListView[EditorRow] | Null = null
    var scrollHandle: SetIntervalHandle | Null = null

    def stopAutoScroll(): Unit =
      if (scrollHandle != null) {
        clearInterval(scrollHandle.nn)
        scrollHandle = null
      }

    def refreshHeap(label: String): Unit =
      heapProperty.set(heapLabel(label))

    def forceGcIfAvailable(): Unit = {
      val gc = js.Dynamic.global.selectDynamic("gc")
      if (js.typeOf(gc) == "function") {
        gc.asInstanceOf[js.Function0[Unit]].apply()
      }
    }

    def remount(): Unit = {
      stopAutoScroll()
      mountedProperty.set(true)
      statusProperty.set("Mounted 1000 editor rows. Take the baseline snapshot now.")
      setTimeout(250) {
        refreshHeap("After mount")
      }
    }

    def unmount(): Unit = {
      stopAutoScroll()
      mountedProperty.set(false)
      listView = null
      forceGcIfAvailable()
      statusProperty.set("Unmounted. Force GC in DevTools, then take the comparison snapshot.")
      setTimeout(1200) {
        forceGcIfAvailable()
        refreshHeap("After unmount")
      }
    }

    def startAutoScroll(): Unit = {
      stopAutoScroll()
      if (!mountedProperty.get) {
        remount()
      }

      var index = 0
      statusProperty.set("Auto-scrolling through all 1000 editor rows.")
      scrollHandle = setInterval(80) {
        val current = listView
        if (current == null) {
          stopAutoScroll()
        } else if (index >= rows.length) {
          current.scrollTo(rows.length - 1)
          stopAutoScroll()
          statusProperty.set("Scroll pass complete. Unmount when the heap has settled.")
          refreshHeap("After scroll")
        } else {
          current.scrollTo(index)
          index += 12
        }
      }
    }

    showcasePage(i18n"Memory-Leak-Test", i18n"1000 Lexical editors through a VirtualList lifecycle.") {
      vbox {
        summon[Component].addDisposable(() => stopAutoScroll())
        style { gap = "34px" }

        sectionIntro(
          i18n"Editor lifecycle stress",
          i18n"Mount, recycle, and unmount editor islands until the heap tells the truth.",
          i18n"The test drives 1000 editor rows through VirtualList. Chrome DevTools remains the source of truth for the before/after heap comparison."
        )

        metricStrip(
          DemoI18n.resolveNow(i18n"1000") -> DemoI18n.resolveNow(i18n"editor rows"),
          DemoI18n.resolveNow(i18n"VirtualList") -> DemoI18n.resolveNow(i18n"recycles visible editor islands"),
          DemoI18n.resolveNow(i18n"0 Delta") -> DemoI18n.resolveNow(i18n"the drop-the-mic target")
        )

        componentShowcase(
          i18n"Memory leak rig",
          i18n"Run the scroll pass, unmount the list, then compare heap snapshots in Chrome."
        ) {
          vbox {
            style { gap = "16px" }

            hbox {
              style { gap = "10px"; flexWrap = "wrap"; alignItems = "center" }

              button(DemoI18n.text(i18n"Mount")) {
                onClick { _ => remount() }
              }

              button(DemoI18n.text(i18n"Auto-scroll")) {
                onClick { _ => startAutoScroll() }
              }

              button(DemoI18n.text(i18n"Bottom")) {
                onClick { _ =>
                  Option(listView).foreach(_.scrollTo(rows.length - 1))
                  statusProperty.set("Jumped to the final editor row.")
                  refreshHeap("At bottom")
                }
              }

              button(DemoI18n.text(i18n"Unmount")) {
                onClick { _ => unmount() }
              }

              button(DemoI18n.text(i18n"Heap")) {
                onClick { _ =>
                  forceGcIfAvailable()
                  refreshHeap("Manual")
                }
              }
            }

            hbox {
              style { gap = "14px"; flexWrap = "wrap" }
              statusCard(DemoI18n.text(i18n"Status"), statusProperty)
              statusCard(DemoI18n.text(i18n"Chrome heap"), heapProperty)
            }

            condition(mountedProperty) {
              thenDo {
                vbox {
                  style {
                    height = "640px"
                    border = "1px solid var(--aj-line)"
                    borderRadius = "8px"
                    overflow = "hidden"
                    backgroundColor = "var(--aj-surface)"
                  }

                  listView = virtualList(rows, estimateHeightPx = 420, overscanPx = 360, prefetchItems = 48) { (itemOrNull, index) =>
                    val item = itemOrNull.asInstanceOf[EditorRow | Null]
                    if (item == null) {
                      div {
                        style {
                          height = "420px"
                          padding = "18px"
                          color = "var(--aj-ink-muted)"
                          boxSizing = "border-box"
                        }
                        text = s"$index - Loading..."
                      }
                    } else {
                      editorRow(item.nn)
                    }
                  }
                }
              }

              elseDo {
                div {
                  classes = Seq("showcase-result")
                  style {
                    minHeight = "180px"
                    display = "flex"
                    alignItems = "center"
                    justifyContent = "center"
                    color = "var(--aj-ink-muted)"
                  }
                  text = DemoI18n.text(i18n"VirtualList unmounted.")
                }
              }
            }
          }
        }

        apiSection(
          i18n"Chrome protocol",
          i18n"Use DevTools Memory snapshots around the same UI lifecycle every time."
        ) {
          codeBlock("text", """1. Open /memory-leak-test in Chrome.
2. DevTools > Memory > Collect garbage > Take snapshot.
3. Click Auto-scroll and wait for completion.
4. Click Unmount, then Collect garbage again.
5. Take the second snapshot and compare retained objects.""")
        }
      }
    }
  }

  private def editorRow(item: EditorRow): Unit = {
    vbox {
      style {
        height = "420px"
        gap = "12px"
        padding = "16px"
        borderBottom = "1px solid var(--aj-line-faint)"
        boxSizing = "border-box"
      }

      div {
        style {
          display = "flex"
          justifyContent = "space-between"
          gap = "12px"
          fontWeight = "800"
        }
        text = item.title
      }

      editor(s"memory-leak-editor-${item.id}", standalone = true) {
        value = item.value
        placeholder = "Stress editor"
        style {
          width = "100%"
          minHeight = "310px"
        }
        installStressPlugins()
      }
    }
  }

  private def installStressPlugins()(using jfx.form.Editor): Unit = {
    basePlugin()
    headingPlugin()
    listPlugin()
    linkPlugin()
    imagePlugin()
    tablePlugin()
    codePlugin()
  }

  private def lexicalState(text: String): js.Any =
    js.Dynamic.literal(
      root = js.Dynamic.literal(
        `type` = "root",
        version = 1,
        indent = 0,
        children = js.Array(
          js.Dynamic.literal(
            `type` = "paragraph",
            version = 1,
            indent = 0,
            children = js.Array(
              js.Dynamic.literal(
                `type` = "text",
                version = 1,
                text = text,
                detail = 0,
                format = 0,
                mode = "normal"
              )
            )
          )
        )
      )
    )

  private def statusCard(title: ReadOnlyProperty[String], body: ReadOnlyProperty[String]): Unit = {
    vbox {
      classes = Seq("showcase-result")
      style { gap = "8px"; flex = "1 1 260px" }
      div {
        style { fontWeight = "800" }
        text = title
      }
      div {
        style {
          color = "var(--aj-ink-muted)"
        }
        text = body
      }
    }
  }

  private def heapLabel(label: String): String = {
    if (RenderBackend.current.isServer) {
      return s"$label: Chrome-only"
    }

    val memory = dom.window.performance.asInstanceOf[js.Dynamic].selectDynamic("memory")
    if (js.typeOf(memory) == "undefined" || memory == null) {
      s"$label: performance.memory unavailable"
    } else {
      val used = memory.selectDynamic("usedJSHeapSize").asInstanceOf[Double]
      val total = memory.selectDynamic("totalJSHeapSize").asInstanceOf[Double]
      f"$label: ${used / 1024 / 1024}%.1f MB used / ${total / 1024 / 1024}%.1f MB total"
    }
  }
}
