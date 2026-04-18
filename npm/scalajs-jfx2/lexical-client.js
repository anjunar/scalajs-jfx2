let lexicalModulesPromise

async function importLexicalModules() {
  // scalajs-lexical owns these runtime dependencies. Until it exposes a single
  // browser runtime namespace, keep the upstream imports localized to this bridge.
  const [
    scalajsLexical,
    lexical,
    richText,
    history,
    list,
    link,
    code,
    selection,
  ] = await Promise.all([
    import('@anjunar/scalajs-lexical'),
    import('lexical'),
    import('@lexical/rich-text'),
    import('@lexical/history'),
    import('@lexical/list'),
    import('@lexical/link'),
    import('@lexical/code'),
    import('@lexical/selection'),
  ])

  return {
    ...scalajsLexical,
    ...lexical,
    ...richText,
    ...history,
    ...list,
    ...link,
    ...code,
    ...selection,
  }
}

function getLexicalModules() {
  lexicalModulesPromise ??= importLexicalModules()
  return lexicalModulesPromise
}

export async function mountLexicalEditor(host, options = {}) {
  const lexical = await getLexicalModules()
  const disposables = []
  let suppressChange = false
  let currentValue = normalizeValue(options.value)

  host.replaceChildren()
  host.classList.add('jfx-editor')

  const shell = document.createElement('div')
  shell.className = 'jfx-editor__shell'

  const toolbar = document.createElement('div')
  toolbar.className = 'jfx-editor__toolbar'

  const surfaceWrap = document.createElement('div')
  surfaceWrap.className = 'jfx-editor__surface-wrap'

  const surface = document.createElement('div')
  surface.className = 'jfx-editor__surface lexical-editor-input'
  surface.setAttribute('role', 'textbox')
  surface.setAttribute('aria-multiline', 'true')
  surface.setAttribute('spellcheck', 'true')

  const placeholder = document.createElement('div')
  placeholder.className = 'jfx-editor__placeholder'
  placeholder.textContent = options.placeholder ?? ''

  surfaceWrap.append(surface, placeholder)
  shell.append(toolbar, surfaceWrap)
  host.append(shell)

  const editor = lexical.createEditor({
    namespace: options.namespace ?? 'JFX Lexical Editor',
    editable: options.editable !== false,
    nodes: [
      lexical.HeadingNode,
      lexical.QuoteNode,
      lexical.ListNode,
      lexical.ListItemNode,
      lexical.LinkNode,
      lexical.CodeNode,
      lexical.CodeMirrorNode,
    ].filter(Boolean),
    theme: {
      paragraph: 'lexical-paragraph',
      quote: 'lexical-quote',
      heading: {
        h1: 'lexical-heading-1',
        h2: 'lexical-heading-2',
        h3: 'lexical-heading-3',
      },
      text: {
        bold: 'lexical-text-bold',
        italic: 'lexical-text-italic',
        underline: 'lexical-text-underline',
        strikethrough: 'lexical-text-strikethrough',
        code: 'lexical-text-code',
      },
      code: 'lexical-text-code',
    },
    onError(error) {
      console.error(error)
    },
  })

  editor.setRootElement(surface)

  if (lexical.registerRichText) {
    disposables.push(lexical.registerRichText(editor))
  }

  if (lexical.registerHistory && lexical.createEmptyHistoryState) {
    disposables.push(lexical.registerHistory(editor, lexical.createEmptyHistoryState(), 300))
  }

  if (lexical.registerList) {
    disposables.push(lexical.registerList(editor))
  }

  registerCodeMirrorSupport(editor, lexical, disposables)
  registerDecoratorDomBridge(editor, disposables)

  const toolbarController = mountToolbar(
    toolbar,
    editor,
    lexical,
    options.plugins,
    options.editable !== false
  )
  disposables.push(toolbarController.destroy)

  applyValue(editor, lexical, currentValue)
  updateEditable(editor, surface, toolbarController, options.editable !== false)
  updatePlaceholder(placeholder, editor, lexical, options.placeholder)

  disposables.push(
    editor.registerUpdateListener(({ editorState }) => {
      updatePlaceholder(placeholder, editor, lexical, options.placeholder)
      toolbarController.refreshState()

      if (suppressChange) return

      const json = JSON.stringify(editorState.toJSON())
      if (json === currentValue) return

      currentValue = json
      options.onStateChange?.(json)
    })
  )

  const focusIn = () => options.onFocus?.()
  const focusOut = () => options.onBlur?.()
  surface.addEventListener('focusin', focusIn)
  surface.addEventListener('focusout', focusOut)

  disposables.push(() => surface.removeEventListener('focusin', focusIn))
  disposables.push(() => surface.removeEventListener('focusout', focusOut))

  return {
    setValue(value) {
      const nextValue = normalizeValue(value)
      if (nextValue === currentValue) return

      currentValue = nextValue
      suppressChange = true
      try {
        applyValue(editor, lexical, nextValue)
      } finally {
        queueMicrotask(() => {
          suppressChange = false
          updatePlaceholder(placeholder, editor, lexical, options.placeholder)
        })
      }
    },

    setEditable(editable) {
      updateEditable(editor, surface, toolbarController, editable)
    },

    setPlaceholder(value) {
      options.placeholder = value ?? ''
      updatePlaceholder(placeholder, editor, lexical, options.placeholder)
    },

    setPlugins(plugins) {
      options.plugins = plugins ?? []
      toolbarController.setPlugins(options.plugins)
    },

    focus() {
      editor.focus?.()
    },

    destroy() {
      for (const dispose of disposables.splice(0).reverse()) {
        try {
          dispose?.()
        } catch {
          // Lexical unregister callbacks should be best-effort during teardown.
        }
      }

      editor.setRootElement(null)
      host.replaceChildren()
    },
  }
}

function registerCodeMirrorSupport(editor, lexical, disposables) {
  if (!lexical.CodeMirrorNode || !editor.registerMutationListener || !lexical.handleCodeMirrorMutation) {
    return
  }

  disposables.push(
    editor.registerMutationListener(
      lexical.CodeMirrorNode,
      (mutations) => {
        mutations.forEach((mutation, nodeKey) => {
          if (mutation === 'destroyed') {
            lexical.handleCodeMirrorMutation(nodeKey, mutation, editor)
          }
        })
      },
      {}
    )
  )
}

function registerDecoratorDomBridge(editor, disposables) {
  if (!editor.registerDecoratorListener) return

  disposables.push(
    editor.registerDecoratorListener((decorators) => {
      for (const [nodeKey, decoratorElement] of Object.entries(decorators ?? {})) {
        const nodeContainer = editor.getElementByKey?.(nodeKey)
        if (nodeContainer && decoratorElement && !nodeContainer.contains(decoratorElement)) {
          nodeContainer.replaceChildren(decoratorElement)
        }
      }
    })
  )
}

function mountToolbar(toolbar, editor, lexical, plugins, editable) {
  const state = {
    editable,
    plugins: normalizePlugins(plugins),
    buttons: [],
  }

  const render = () => {
    state.buttons = []

    const groups = groupToolbarElements(flattenToolbarElements(state.plugins))
    toolbar.replaceChildren(...groups.map((group) => toolbarGroup(group, state, editor, lexical)))
    toolbar.hidden = !state.editable || groups.length === 0
    refreshToolbarState(editor, lexical, state)
  }

  render()

  const refreshSelection = () => {
    refreshToolbarState(editor, lexical, state)
    return false
  }

  const unregisterSelection =
    lexical.SELECTION_CHANGE_COMMAND && editor.registerCommand
      ? editor.registerCommand(
          lexical.SELECTION_CHANGE_COMMAND,
          refreshSelection,
          lexical.COMMAND_PRIORITY_LOW ?? 1
        )
      : null

  return {
    setEditable(nextEditable) {
      state.editable = nextEditable
      toolbar.hidden = !state.editable || state.buttons.length === 0
      refreshToolbarState(editor, lexical, state)
    },

    setPlugins(nextPlugins) {
      state.plugins = normalizePlugins(nextPlugins)
      render()
    },

    refreshState() {
      refreshToolbarState(editor, lexical, state)
    },

    destroy() {
      unregisterSelection?.()
      toolbar.replaceChildren()
      state.buttons = []
    },
  }
}

function toolbarGroup(group, state, editor, lexical) {
  const element = document.createElement('div')
  element.className = 'jfx-editor__toolbar-group'
  element.setAttribute('role', 'group')
  element.setAttribute('aria-label', group.label)

  const label = document.createElement('span')
  label.className = 'jfx-editor__toolbar-group-label'
  label.textContent = group.label

  const buttons = document.createElement('div')
  buttons.className = 'jfx-editor__toolbar-buttons'

  for (const item of group.items) {
    buttons.append(toolbarButton(item, state, editor, lexical))
  }

  element.append(label, buttons)
  return element
}

function toolbarButton(item, state, editor, lexical) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'jfx-editor__toolbar-button'
  button.title = item.title || item.label || item.id
  button.setAttribute('aria-label', item.title || item.label || item.id)

  if (item.icon) {
    const icon = document.createElement('span')
    icon.className = 'material-icons jfx-editor__toolbar-icon'
    icon.setAttribute('aria-hidden', 'true')
    icon.textContent = item.icon
    button.append(icon)
  }

  const label = document.createElement('span')
  label.className = 'jfx-editor__toolbar-button-label'
  label.textContent = item.label || item.id
  button.append(label)

  button.addEventListener('click', () => {
    if (!state.editable) return

    executeToolbarCommand(editor, lexical, item.command)
    queueMicrotask(() => refreshToolbarState(editor, lexical, state))
  })

  state.buttons.push({ button, command: item.command })
  return button
}

function updateEditable(editor, surface, toolbarController, editable) {
  editor.setEditable?.(editable)
  surface.contentEditable = String(editable)
  surface.setAttribute('aria-readonly', String(!editable))
  toolbarController.setEditable(editable)
}

function normalizePlugins(plugins) {
  if (!Array.isArray(plugins)) return []

  return plugins
    .filter(Boolean)
    .map((plugin) => ({
      name: String(plugin.name ?? ''),
      toolbarElements: Array.isArray(plugin.toolbarElements) ? plugin.toolbarElements : [],
    }))
}

function flattenToolbarElements(plugins) {
  return plugins.flatMap((plugin) =>
    plugin.toolbarElements
      .filter((item) => item?.type === 'button' && item.command)
      .map((item, index) => ({
        id: String(item.id ?? `${plugin.name}-${index}`),
        label: String(item.label ?? item.id ?? plugin.name),
        title: String(item.title ?? item.label ?? item.id ?? plugin.name),
        icon: String(item.icon ?? ''),
        group: String(item.group ?? plugin.name ?? 'editor'),
        groupLabel: String(item.groupLabel ?? item.group ?? plugin.name ?? 'Editor'),
        command: item.command,
      }))
  )
}

function groupToolbarElements(items) {
  const groups = []
  const groupMap = new Map()

  for (const item of items) {
    if (!groupMap.has(item.group)) {
      const group = { id: item.group, label: item.groupLabel, items: [] }
      groups.push(group)
      groupMap.set(item.group, group)
    }

    groupMap.get(item.group).items.push(item)
  }

  return groups
}

function executeToolbarCommand(editor, lexical, command) {
  if (!command) return

  switch (command.type) {
    case 'undo':
      dispatchIfPresent(editor, lexical.UNDO_COMMAND)
      break

    case 'redo':
      dispatchIfPresent(editor, lexical.REDO_COMMAND)
      break

    case 'formatText':
      if (lexical.FORMAT_TEXT_COMMAND && command.format) {
        editor.dispatchCommand(lexical.FORMAT_TEXT_COMMAND, command.format)
      }
      break

    case 'formatBlock':
      formatBlock(editor, lexical, command.block, command.language)
      break

    case 'insertList':
      insertList(editor, lexical, command.kind)
      break

    case 'toggleLink':
      toggleLink(editor, lexical, command)
      break

    case 'insertCodeMirror':
      insertCodeMirrorBlock(editor, lexical, command)
      break
  }
}

function dispatchIfPresent(editor, command, payload = undefined) {
  if (command) {
    editor.dispatchCommand(command, payload)
  }
}

function formatBlock(editor, lexical, block, language) {
  if (!block) return

  if (block.startsWith('h') && lexical.FORMAT_HEADING_COMMAND) {
    editor.dispatchCommand(lexical.FORMAT_HEADING_COMMAND, block)
    return
  }

  editor.update(() => {
    const selection = lexical.$getSelection?.()
    if (!selection || !lexical.$isRangeSelection?.(selection) || !lexical.$setBlocksType) {
      return
    }

    switch (block) {
      case 'paragraph':
        lexical.$setBlocksType(selection, () => lexical.$createParagraphNode())
        break

      case 'quote':
        if (lexical.$createQuoteNode) {
          lexical.$setBlocksType(selection, () => lexical.$createQuoteNode())
        }
        break

      case 'code':
        if (lexical.$createCodeNode) {
          lexical.$setBlocksType(selection, () => {
            const node = lexical.$createCodeNode(language || 'javascript')
            node.setLanguage?.(language || 'javascript')
            return node
          })
        }
        break
    }
  })
}

function insertList(editor, lexical, kind) {
  if (kind === 'number') {
    dispatchIfPresent(editor, lexical.INSERT_ORDERED_LIST_COMMAND)
  } else {
    dispatchIfPresent(editor, lexical.INSERT_UNORDERED_LIST_COMMAND)
  }
}

function toggleLink(editor, lexical, command) {
  if (!lexical.TOGGLE_LINK_COMMAND) return

  const currentUrl = currentLinkUrl(editor, lexical)
  const fallbackUrl = currentUrl || command.defaultUrl || 'https://'
  const nextUrl = window.prompt(command.prompt || 'Link URL', fallbackUrl)
  if (nextUrl == null) return

  const trimmed = nextUrl.trim()
  editor.dispatchCommand(lexical.TOGGLE_LINK_COMMAND, trimmed ? trimmed : null)
}

function insertCodeMirrorBlock(editor, lexical, command) {
  const defaultLanguage = command.language || 'scala'
  const language =
    command.promptLanguage === false
      ? defaultLanguage
      : window.prompt('Code language', defaultLanguage)

  if (language == null) return

  if (lexical.$createCodeMirrorNode && lexical.$insertNodes) {
    editor.update(() => {
      lexical.$insertNodes([lexical.$createCodeMirrorNode('', language.trim() || defaultLanguage)])
      editor.focus?.()
    })
    return
  }

  formatBlock(editor, lexical, 'code', language.trim() || defaultLanguage)
}

function refreshToolbarState(editor, lexical, state) {
  if (!state.buttons.length) return

  editor.getEditorState().read(() => {
    for (const entry of state.buttons) {
      const active = isToolbarCommandActive(lexical, entry.command)
      entry.button.classList.toggle('is-active', active)
      entry.button.classList.toggle('active', active)
      entry.button.setAttribute('aria-pressed', String(active))
      entry.button.disabled = !state.editable
    }
  })
}

function isToolbarCommandActive(lexical, command) {
  if (!command) return false

  switch (command.type) {
    case 'formatText':
      return selectionHasTextFormat(lexical, command.format)

    case 'formatBlock':
      return selectedBlockType(lexical) === command.block

    case 'insertList':
      return selectedListType(lexical) === command.kind

    case 'toggleLink':
      return selectionIsLink(lexical)

    default:
      return false
  }
}

function selectionHasTextFormat(lexical, format) {
  const selection = lexical.$getSelection?.()
  return Boolean(selection && lexical.$isRangeSelection?.(selection) && selection.hasFormat?.(format))
}

function selectedBlockType(lexical) {
  const element = selectedTopLevelElement(lexical)
  if (!element) return ''

  if (typeof element.getTag === 'function') {
    return element.getTag()
  }

  if (typeof element.getType === 'function') {
    const type = element.getType()
    return type === 'paragraph' ? 'paragraph' : type
  }

  return ''
}

function selectedListType(lexical) {
  let element = selectedTopLevelElement(lexical)

  while (element) {
    if (lexical.$isListNode?.(element)) {
      const listType = element.getListType?.()
      return listType === 'number' ? 'number' : 'bullet'
    }

    element = element.getParent?.()
  }

  return ''
}

function selectionIsLink(lexical) {
  const selection = lexical.$getSelection?.()
  if (!selection || !lexical.$isRangeSelection?.(selection) || !lexical.$isLinkNode) {
    return false
  }

  return selection.getNodes().some((node) => lexical.$isLinkNode(node) || lexical.$isLinkNode(node.getParent?.()))
}

function currentLinkUrl(editor, lexical) {
  if (!lexical.$isLinkNode) return ''

  let url = ''

  editor.getEditorState().read(() => {
    const selection = lexical.$getSelection?.()
    if (!selection || !lexical.$isRangeSelection?.(selection)) return

    const link = selection
      .getNodes()
      .map((node) => (lexical.$isLinkNode(node) ? node : node.getParent?.()))
      .find((node) => node && lexical.$isLinkNode(node))

    url = link?.getURL?.() ?? ''
  })

  return url
}

function selectedTopLevelElement(lexical) {
  const selection = lexical.$getSelection?.()
  if (!selection || !lexical.$isRangeSelection?.(selection)) return null

  const anchor = selection.anchor?.getNode?.()
  if (!anchor) return null

  if (anchor.getKey?.() === 'root') {
    return anchor
  }

  return anchor.getTopLevelElementOrThrow?.() ?? null
}

function updatePlaceholder(placeholder, editor, lexical, text) {
  placeholder.textContent = text ?? ''

  editor.getEditorState().read(() => {
    const root = lexical.$getRoot()
    placeholder.hidden = !text || !root.isEmpty()
  })
}

function applyValue(editor, lexical, value) {
  if (value) {
    try {
      editor.setEditorState(editor.parseEditorState(value))
      return
    } catch {
      // Non-JSON strings are treated as plain text for convenience.
    }
  }

  editor.update(() => {
    const root = lexical.$getRoot()
    root.clear()

    const paragraph = lexical.$createParagraphNode()
    if (value) {
      paragraph.append(lexical.$createTextNode(value))
    }
    root.append(paragraph)
  })
}

function normalizeValue(value) {
  if (value == null) return ''
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}
