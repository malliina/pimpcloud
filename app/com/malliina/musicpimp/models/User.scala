package com.malliina.musicpimp.models

case class User(name: String) {
  override def toString = name
}

object User extends SimpleCompanion[String, User] {
  override def raw(t: User): String = t.name
}
