package org.presheaf.ops

import java.io.File

case class Diagram(
              id: String,
              source: String,
              img: File,
              pdf: File,
              log: Iterable[String] = Nil) {
  override def toString: String =
    s"Diagram($id, $source, $img, $pdf, ${log.mkString("\n")})"
}

object Diagram {
  def bad(explanation: String): Nothing = throw Bad(explanation)

  case class Bad(explanation: String) extends RuntimeException(explanation)
}