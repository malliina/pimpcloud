package controllers

import com.malliina.play.controllers.{AccountController, BaseSecurity}
import play.api.Logger

class Secured(auth: BaseSecurity) extends AccountController(auth) with PimpContentController

object Secured {
  private val log = Logger(getClass)
}
