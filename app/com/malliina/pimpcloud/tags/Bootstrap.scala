package com.malliina.pimpcloud.tags

import com.malliina.pimpcloud.tags.Tags._

import scalatags.Text.all._

object Bootstrap extends Bootstrap

/**
  * Scalatags for Twitter Bootstrap.
  */
trait Bootstrap {
  val Alert = "alert"
  val AlertSuccess = s"$Alert $Alert-success"
  val AlertWarning = s"$Alert $Alert-warning"
  val Btn = "btn"
  val BtnGroup = s"$Btn-group"
  val BtnPrimary = s"$Btn $Btn-primary"
  val BtnDefault = s"$Btn $Btn-default"
  val BtnLg = "btn-lg"
  val BtnXs = "btn-xs"
  val BtnBlock = "btn-block"
  val Collapse = "collapse"
  val ColMd3 = "col-md-3"
  val ColMd4 = "col-md-4"
  val ColMd6 = "col-md-6"
  val ColMd8 = "col-md-8"
  val ColMd9 = "col-md-9"
  val ColMd12 = "col-md-12"
  val ColMdOffset2 = "col-md-offset-2"
  val Container = "container"
  val Dropdown = "dropdown"
  val DropdownMenu = "dropdown-menu"
  val DropdownToggle = "dropdown-toggle"
  val Jumbotron = "jumbotron"
  val Nav = "nav"
  val NavStacked = s"$Nav $Nav-stacked"
  val Navbar = "navbar"
  val NavbarBrand = "navbar-brand"
  val NavbarCollapse = "navbar-collapse"
  val NavbarHeader = "navbar-header"
  val NavbarDefault = "navbar-default"
  val NavbarNav = "navbar-nav"
  val NavbarRight = "navbar-right"
  val NavbarToggle = "navbar-toggle"
  val PageHeader = "page-header"
  val PullLeft = "pull-left"
  val PullRight = "pull-right"
  val Row = "row"
  val VisibleLg = "visible-lg"
  val VisibleMd = "visible-md"
  val VisibleSm = "visible-sm"

  def headerRow(clazz: String = ColMd12)(header: Modifier*) =
    rowColumn(clazz)(
      divClass(PageHeader)(
        h1(header)
      )
    )

  def fullRow(inner: Modifier*) = rowColumn(ColMd12)(inner)

  def rowColumn(clazz: String)(inner: Modifier*) = row(divClass(clazz)(inner))

  def row = divClass(Row)

  def div4 = divClass(ColMd4)

  def glyphIcon(glyphName: String) = iClass(s"glyphicon glyphicon-$glyphName")
}
