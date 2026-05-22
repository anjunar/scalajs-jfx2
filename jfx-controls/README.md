# scalajs-jfx2-controls

Controls contains reusable UI components that are not form fields: links, images, tables and virtual lists.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.2.4"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-router" % "2.2.4"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-controls" % "2.2.4"
```

## Link

`Link` resolves hrefs through `RouterConfig`.

```scala
import jfx.control.Link.link
import jfx.core.component.Component.*

link("/docs") {
  text = "Documentation"
}
```

## Image

```scala
import jfx.control.Image.image

image {
  src = "/assets/avatar.png"
  alt = "User avatar"
}
```

Reactive image sources are supported.

```scala
import jfx.core.state.Property

val currentSrc = Property("/assets/a.png")
val currentAlt = Property("Current image")

image {
  src = currentSrc
  alt = currentAlt
}
```

## TableView

```scala
import jfx.control.TableColumn.column
import jfx.control.TableView.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div

final case class Book(title: String, author: String, year: Int)

val books = ListProperty[Book]()
books.setAll(Seq(
  Book("Dune", "Frank Herbert", 1965),
  Book("Kindred", "Octavia Butler", 1979)
))

tableView[Book] {
  items = books
  rowHeight = 48
  crawlable = true

  column[Book, String]("Title") { book =>
    div { text = book.title }
  }

  column[Book, String]("Author") { book =>
    div { text = book.author }
  }

  column[Book, Int]("Year") {
    sortable = true
    sortKey = "year"
    cellRenderer = book => div { text = book.year.toString }
  }
}
```

## TableColumn

Columns can be configured separately.

```scala
column[Book, String]("Title") {
  prefWidth = 260
  sortable = true
  sortKey = "title"
  cellRenderer = book => div { text = book.title }
}
```

## TableRow And TableCell

Rows and cells are normally created by `TableView`, but the DSL components are available for custom composition and tests.

```scala
import jfx.control.TableCell.cell
import jfx.control.TableRow.tableRow

tableRow[Book] {
  cell[Book, String] {
    text = "Standalone cell"
  }
}
```

## VirtualListView

```scala
import jfx.control.VirtualListView.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div

val rows = ListProperty[String]()
rows.setAll((1 to 10000).map(i => s"Row $i"))

virtualList[String] {
  items = rows
  estimateHeightPx = 44
  overscanPx = 240
  crawlable = true
  cellRenderer = { (row: String | Null, index: Int) =>
    div {
      classes = Seq("row")
      text = if (row == null) s"Loading $index" else row
    }
  }
}
```

## DataGrid

`DataGrid` virtualizes predictable card grids. `itemWidthPx` defines the preferred card width used to pick the column count, while the rendered cards stretch across the measured viewport. `itemHeightPx` remains fixed so the grid can keep row math deterministic.

```scala
import jfx.control.DataGrid.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div

val posts = ListProperty[String]()
posts.setAll((1 to 10000).map(i => s"Post $i"))

dataGrid[String] {
  items = posts
  itemWidthPx = 320
  itemHeightPx = 220
  gapPx = 16
  crawlable = true
  cellRenderer = { (post: String | Null, index: Int) =>
    div {
      classes = Seq("post-card")
      text = if (post == null) s"Loading $index" else post
    }
  }
}
```

Loading and empty placeholders can also be provided directly through the DSL.

```scala
val grid = dataGrid[String] {
  items = posts
  itemWidthPx = 320
  itemHeightPx = 220
  gapPx = 16
  cellRenderer = { (post: String | Null, index: Int) =>
    div {
      classes = Seq("post-card")
      text = if (post == null) s"Loading $index" else post
    }
  }
  loadingPlaceholder {
    div {
      classes = Seq("post-grid-loading")
      text = "Loading articles..."
    }
  }
  emptyPlaceholder {
    div {
      classes = Seq("post-grid-empty")
      text = "No articles available."
    }
  }
}
```

## Carousel

`Carousel` keeps a single active slide in the browser, wraps back to the first slide after the last one, and can advance itself on a timer. For SSR it can either expose every slide state in one server render or only the active slide.

```scala
import jfx.control.Carousel.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div

val slides = ListProperty[String]()
slides.setAll(Seq("North", "East", "South"))

carousel(slides, autoAdvanceMs = 2800, ssrShowAllStates = true) { (slide, index) =>
  div {
    classes = Seq("hero-slide")
    text = s"${index + 1}. $slide"
  }
}
```

## Remote Lists

`TableView`, `VirtualListView`, and `DataGrid` can consume `RemoteListProperty`.

```scala
import jfx.core.state.ListProperty
import scala.scalajs.js

final case class Page(index: Int, limit: Int)

val remoteRows = ListProperty.remote[String, Page](
  loader = ListProperty.RemoteLoader { query =>
    js.Promise.resolve(
      ListProperty.RemotePage(
        items = Seq("A", "B"),
        offset = Some(query.index),
        nextQuery = Some(Page(query.index + query.limit, query.limit)),
        totalCount = Some(100),
        hasMore = Some(true)
      )
    )
  },
  initialQuery = Page(0, 50)
)
```
