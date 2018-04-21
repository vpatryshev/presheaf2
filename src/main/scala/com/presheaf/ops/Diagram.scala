package com.presheaf.ops

import java.io.File

case class Diagram(
    id: String,
    source: String,
    img: File,
    pdf: File,
    log: String = ""
) {
  def inCache: Boolean = img.exists && pdf.exists

  override def toString: String =
    s"Diagram($id, $source, $img, $pdf, $log)"
}

object Diagram {
  def bad(explanation: String): Nothing = throw Bad(explanation)

  case class Bad(explanation: String) extends RuntimeException(explanation)
}