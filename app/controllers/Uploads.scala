package controllers

import play.api.libs.Files
import play.api.mvc._

class Uploads(parser: BodyParser[MultipartFormData[Files.TemporaryFile]] = BodyParsers.parse.multipartFormData)
  extends Controller {

  def up = Action(parser) { req =>
    val lengths = req.body.files.map(file => s"${file.filename}: ${file.ref.file.length()}")
    Ok(lengths mkString "\n")
  }
}
