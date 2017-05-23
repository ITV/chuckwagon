package com.itv.chuckwagon.lambda

import java.io.InputStream
import java.io.OutputStream

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import io.circe.Decoder
import io.circe.Encoder

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

abstract class Handler[T, R](implicit decoder: Decoder[T], encoder: Encoder[R]) extends RequestStreamHandler {
  import Encoding._

  val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  protected def handler(input: T, context: Context): R
  def handleRequest(is: InputStream, os: OutputStream, context: Context): Unit =
    in(is).flatMap(i => out(handler(i, context), os)).get
}

abstract class FutureHandler[T, R](d: Option[Duration] = None)(
    implicit decoder: Decoder[T],
    encoder: Encoder[R],
    ec: ExecutionContext
) extends Handler[T, R] {
  protected def handlerFuture(input: T, context: Context): Future[R]
  protected def handler(input: T, context: Context): R = Await.result(
    handlerFuture(input, context),
    d.getOrElse(Duration(context.getRemainingTimeInMillis.toLong, MILLISECONDS))
  )
}
