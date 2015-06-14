package com.mle.pimpcloud.ws

import com.mle.musicpimp.audio.Track
import com.mle.play.ContentRange
import rx.lang.scala.Subject

/**
 * @author Michael
 */
case class StreamInfo(track: Track, range: ContentRange, stream: Subject[Array[Byte]])
