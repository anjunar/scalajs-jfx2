package jfx.form.editor.plugins

import lexical.DialogService
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

final class DefaultDialogService extends DialogService {

  override def show(title: String, contentProvider: () => HTMLElement, onConfirm: HTMLElement => Unit): Unit = {
    val backdrop = dom.document.createElement("div").asInstanceOf[HTMLElement]
    backdrop.className = "jfx-dialog-backdrop"

    val modal = dom.document.createElement("div").asInstanceOf[HTMLElement]
    modal.className = "jfx-dialog"

    val titleEl = dom.document.createElement("h3").asInstanceOf[HTMLElement]
    titleEl.className = "jfx-dialog__title"
    titleEl.textContent = title
    modal.appendChild(titleEl)

    val content = contentProvider()
    content.classList.add("jfx-dialog__content")
    modal.appendChild(content)

    val actions = dom.document.createElement("div").asInstanceOf[HTMLElement]
    actions.className = "jfx-dialog__actions"

    def close(): Unit =
      if (dom.document.body.contains(backdrop)) {
        dom.document.body.removeChild(backdrop)
      }

    val cancelButton = dom.document.createElement("button").asInstanceOf[dom.HTMLButtonElement]
    cancelButton.className = "jfx-dialog__button jfx-dialog__button--secondary"
    cancelButton.textContent = "Cancel"
    cancelButton.onclick = (_: dom.MouseEvent) => close()

    val confirmButton = dom.document.createElement("button").asInstanceOf[dom.HTMLButtonElement]
    confirmButton.className = "jfx-dialog__button jfx-dialog__button--primary"
    confirmButton.textContent = "Confirm"
    confirmButton.onclick = (_: dom.MouseEvent) => {
      onConfirm(content)
      close()
    }

    actions.appendChild(cancelButton)
    actions.appendChild(confirmButton)
    modal.appendChild(actions)
    backdrop.appendChild(modal)
    dom.document.body.appendChild(backdrop)

    backdrop.onclick = (event: dom.MouseEvent) =>
      if (event.target == backdrop) close()
  }

}
