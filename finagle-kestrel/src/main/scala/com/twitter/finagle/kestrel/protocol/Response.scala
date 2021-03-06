package com.twitter.finagle.kestrel.protocol

import org.jboss.netty.buffer.ChannelBuffer

sealed abstract class Response
case class NotFound()                  extends Response
case class Stored()                    extends Response
case class Deleted()                   extends Response
case class Error()                     extends Response

case class Values(values: Seq[Value])  extends Response
case class Value(key: ChannelBuffer, value: ChannelBuffer)
