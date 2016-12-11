package com.malliina.pimpcloud.js

import upickle.{Invalid, Js}

object PimpJSON extends upickle.AttributeTagged {
  override implicit def OptionW[T: Writer]: Writer[Option[T]] = Writer {
    case None => Js.Null
    case Some(s) => implicitly[Writer[T]].write(s)
  }

  override implicit def OptionR[T: Reader]: Reader[Option[T]] = Reader {
    case Js.Null => None
    case v: Js.Value => Some(implicitly[Reader[T]].read.apply(v))
  }

  def validate[T: Reader](expr: String): Either[Invalid, T] =
    try {
      val jsValue = PimpJSON.read[Js.Value](expr)
      validateJs[T](jsValue)
    } catch {
      case e: Invalid => Left(e)
    }

  def validateJs[T: Reader](v: Js.Value): Either[Invalid, T] =
    try {
      Right(readJs[T](v))
    } catch {
      case e: Invalid => Left(e)
    }
}
