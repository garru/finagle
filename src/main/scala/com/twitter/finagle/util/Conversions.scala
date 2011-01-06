package com.twitter.finagle.util

import org.jboss.netty.util.Timer
import org.jboss.netty.channel.ChannelFuture

import com.twitter.util.Future

object Conversions {
  implicit def channelFutureToRichChannelFuture(f: ChannelFuture) = new RichChannelFuture(f)
  implicit def timerToRichTimer(t: Timer) = new RichTimer(t)
  implicit def futureToRichFuture[A](f: Future[A]) = new RichFuture(f)
}
