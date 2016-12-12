package controllers

import com.malliina.musicpimp.audio.{Directory, Folder, Track}
import com.malliina.pimpcloud.models.TrackID
import com.malliina.pimpcloud.tags.Bootstrap._
import com.malliina.pimpcloud.tags.TagPage
import com.malliina.pimpcloud.tags.Tags._
import controllers.routes.Assets.at
import play.api.mvc.Call

import scalatags.Text.GenericAttr
import scalatags.Text.all._

object CloudTags {
  implicit val callAttr = new GenericAttr[Call]
  val empty: Modifier = ""
  val titleTag = tag("title")
  val section = tag("section")
  val nav = tag("nav")

  val logs = baseIndex("logs")(
    headerRow()("Logs ", small(`class` := "pull-right", id := "status")("Initializing...")),
    fullRow(
      defaultTable("logTableBody", "Time", "Message", "Logger", "Thread", "Level")
    )
  )

  def index(dir: Directory, feedback: Option[String]) = {
    val feedbackHtml = feedback.fold(empty)(f => fullRow(leadPara(f)))

    def folderHtml(folder: Folder) =
      li(aHref(routes.Phones.folder(folder.id), folder.title))

    def trackHtml(track: Track) =
      li(trackActions(track.id), " ", aHref(routes.Phones.track(track.id), track.title))

    basePage("Home")(
      div(Container)(
        headerRow()("Library"),
        fullRow(
          searchForm()
        ),
        fullRow(
          p(id := "status")
        ),
        feedbackHtml,
        fullRow(
          ulClass("list-unstyled")(
            dir.folders map folderHtml,
            dir.tracks map trackHtml
          )
        )
      )
    )
  }

  def trackActions(track: TrackID) =
    divClass(BtnGroup)(
      a(`class` := s"$BtnDefault $BtnXs", href := "#", onclick := s"return play('$track');")(glyphIcon("play"), " Play"),
      a(`class` := s"$BtnDefault $BtnXs dropdown-toggle", attr("data-toggle") := "dropdown", href := "#")(spanClass("caret")),
      ulClass("dropdown-menu")(
        li(a(href := "#", onclick := s"return add('$track');")(glyphIcon("plus"), " Add to playlist")),
        li(aHref(routes.Phones.track(track), glyphIcon("download"), " Download"))
      )
    )

  def searchForm(query: Option[String] = None, size: String = "input-group-lg") = {
    form(action := routes.Phones.search())(
      divClass(s"input-group $size")(
        input(`type` := "text", `class` := "form-control", placeholder := query.getOrElse("Artist, album or track..."), name := "term", id := "term"),
        divClass("input-group-btn")(
          button(`class` := BtnDefault, `type` := "submit")(glyphIcon("search"))
        )
      )
    )
  }

  val admin = baseIndex("home")(
    headerRow()(
      "Admin", small(`class` := "pull-right", id := "status")("Initializing...")
    ),
    tableContainer("Streams", "requestsTable", "Cloud ID", "Request ID", "Track", "Artist", "Bytes"),
    tableContainer("Phones", "phonesTable", "Cloud ID", "Phone Address"),
    tableContainer("Servers", "serversTable", "Cloud ID", "Server Address")
  )

  def tableContainer(header: String, bodyId: String, headers: String*): Modifier = Seq(
    h2(header),
    fullRow(
      defaultTable(bodyId, headers: _*)
    )
  )

  def defaultTable(bodyId: String, headers: String*) =
    table(`class` := "table table-striped table-hover")(
      thead(
        tr(
          headers map { header => th(header) }
        )
      ),
      tbody(id := bodyId)
    )

  def baseIndex(tabName: String)(inner: Modifier*) = {
    def navItem(thisTabName: String, tabId: String, url: Call, glyphiconName: String) = {
      val maybeActive = if (tabId == tabName) Option(`class` := "active") else None
      li(maybeActive)(a(href := url)(glyphIcon(glyphiconName), s" $thisTabName"))
    }

    basePage("pimpcloud")(
      div(s"$Navbar $NavbarDefault")(
        div(Container)(
          div(NavbarHeader)(
            hamburgerButton,
            a(`class` := NavbarBrand, href := routes.UsageStreaming.index())("MusicPimp")
          ),
          div(s"$NavbarCollapse $Collapse")(
            ulClass(s"$Nav $NavbarNav")(
              navItem("Home", "home", routes.UsageStreaming.index(), "home"),
              navItem("Logs", "logs", routes.Logs.logs(), "list")
            ),
            ulClass(s"$Nav $NavbarNav $NavbarRight")(
              li(aHref(routes.AdminAuth.logout(), "Logout"))
            )
          )
        )
      ),
      div(Container)(inner)
    )
  }

  def basePage(title: String)(inner: Modifier*) = TagPage(
    html(lang := "en")(
      head(
        titleTag(title),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
        cssLink("//netdna.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css"),
        cssLink("//netdna.bootstrapcdn.com/font-awesome/3.2.1/css/font-awesome.css"),
        cssLink("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css"),
        cssLink(at("css/custom.css")),
        js("//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"),
        js("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"),
        js("//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js")
      ),
      body(
        section(
          inner,
          js(at("frontend-fastopt.js")),
          js(at("frontend-launcher.js"))
        )
      )
    )
  )

  def hamburgerButton =
    button(`class` := NavbarToggle, attr("data-toggle") := Collapse, attr("data-target") := s".$NavbarCollapse")(
      spanClass("icon-bar"),
      spanClass("icon-bar"),
      spanClass("icon-bar")
    )
}
