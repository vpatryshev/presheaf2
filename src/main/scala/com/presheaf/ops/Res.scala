package com.presheaf.ops

import java.io.InputStream

import scala.io.{ BufferedSource, Source }

object Res {
  def stream(resource: String): InputStream = getClass.getResourceAsStream(resource)

  def read(resource: String): BufferedSource =
    Source.fromInputStream(stream(resource))

  def string(resource: String): String = try {
    Source.fromInputStream(stream(resource)).getLines mkString "\n"
  } catch {
    case npe: NullPointerException =>
      throw new IllegalArgumentException(s"$resource missing in resources")
  }
}