package com.itv.chuckwagon.lambda

import java.io.InputStream
import java.io.OutputStream

import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object Encoding {
  import io.circe._
  import io.circe.parser._
  import io.circe.syntax._

  def in[T](is: InputStream)(implicit decoder: Decoder[T]): Try[T] = {
    val t = Try(Source.fromInputStream(is).mkString).flatMap { string =>
      decode[T](string).fold(Failure(_), Success(_))
    }
    is.close()
    t
  }

  def out[T](value: T, os: OutputStream)(implicit encoder: Encoder[T]): Try[Unit] = {
    val t = Try(os.write(value.asJson.noSpaces.getBytes("UTF-8")))
    os.close()
    t
  }
}
