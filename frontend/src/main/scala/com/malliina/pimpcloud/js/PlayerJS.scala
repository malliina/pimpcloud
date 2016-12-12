package com.malliina.pimpcloud.js

case class PlayerCommand(cmd: String, track: String)

object PlayerCommand {
  def play(id: String) = apply("play", id)

  def add(id: String) = apply("add", id)
}

class PlayerJS extends SocketJS("/mobile/ws") {
  override def handlePayload(payload: String) = {
    setFeedback(payload)
  }

  def add(id: String) = send(PlayerCommand.add(id))

  def play(id: String) = send(PlayerCommand.play(id))

  def send(cmd: PlayerCommand) = socket send PimpJSON.write(cmd)
}
