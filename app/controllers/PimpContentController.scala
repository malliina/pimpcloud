package controllers

import controllers.PimpContentController.log
import play.api.Logger
import play.api.http.MimeTypes
import play.api.libs.json.JsValue
import play.api.mvc.{Controller, RequestHeader, Result}

import scala.concurrent.Future

/** Methods that choose the correct response to provide to clients
  * based on what they accept (HTML/JSON/which JSON version).
  */
trait PimpContentController extends Controller {

  import com.malliina.musicpimp.json.JsonFormatVersions._

  def pimpResponse(html: => Result, json17: => JsValue, latest: => JsValue)(implicit request: RequestHeader): Result = {
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(JSONv17) => Ok(json17)
      case Some(JSONv18) => Ok(latest)
      //      case Some(JSONv24) => Ok(latest)
      case Some(other) =>
        log.warn(s"Client requests unknown response format: $other")
        NotAcceptable
      case None =>
        log.warn("No requested response format, unacceptable.")
        NotAcceptable
    }
  }

  def pimpResult(html: => Result, json: => Result)(implicit request: RequestHeader): Result =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains "json" => json
      case _ => NotAcceptable
    }

  // TODO dry
  def pimpResult(html: => Future[Result], json: => Future[Result])(implicit request: RequestHeader): Future[Result] =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains "json" => json
      case _ => Future.successful(NotAcceptable)
    }

  def pimpResponse(html: => Result, json: => JsValue)(implicit request: RequestHeader): Result =
    pimpResult(html, Ok(json))

  def respond(html: => play.twirl.api.Html, json: => JsValue, status: Status = Ok)(implicit request: RequestHeader): Result =
    pimpResult(status(html), status(json))

  /**
    *
    * @return the equivalent of "Unit" in JSON and HTML
    */
  def AckResponse(implicit request: RequestHeader) =
    pimpResult(html = Accepted, json = Accepted)
}

object PimpContentController {
  private val log = Logger(getClass)
}
