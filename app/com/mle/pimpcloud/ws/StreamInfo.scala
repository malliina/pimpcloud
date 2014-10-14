package com.mle.pimpcloud.ws

import com.mle.musicpimp.audio.Track
import rx.lang.scala.Subject

/**
 * @author Michael
 */
case class StreamInfo(track: Track, stream: Subject[Array[Byte]])