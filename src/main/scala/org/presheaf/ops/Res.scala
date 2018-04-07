package org.presheaf.ops

import java.io.InputStream

import scala.io.{BufferedSource, Source}

object Res {
  def stream(resource: String): InputStream = getClass.getResourceAsStream(resource)
  def read(resource: String): BufferedSource = Source.fromInputStream(stream(resource))
}