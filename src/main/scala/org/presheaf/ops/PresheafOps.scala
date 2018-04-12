package org.presheaf.ops

import org.presheaf._

import java.io._
import BuildInfo._

/**
 * Presheaf operations
 */

trait PresheafOps {
  val cacheDirectory: File
  val renderingScript: String = s"${OS.homeDir}/presheaf.sh"

  def ref(file: File): String = "cache/" + file.getName
  private val xyError = ".*Xy-pic error:(.*)\\\\xyerror.*".r

  val Q = "\""
  def quote(s: String): String = Q + s.replaceAll(Q, "").replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n") + Q
  def json(s: String): String = quote(s)
  def json(nvp: (String, _)): String = json(nvp._1) + ":" + json(nvp._2.toString)
  def json(map: Map[String, _]): String = map.map(json).mkString("{", ",", "}")
  def json(seq: Iterator[String]): String = seq.map(json).mkString("[", ",\n", "]")
  def json(seq: Iterable[String]): String = json(seq.iterator)

  def errorLog(log: String): Map[String, String] = {
    val fullLog = log.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"")
    OS.log(fullLog)
    fullLog match {
      case xyError(msg) =>
        Map(
          "error" -> msg,
          "version" -> version
        )
      case _ =>
        Map(
          "error" -> fullLog,
          "version" -> version
        )

    }
  }

  def process(diagram: String): Diagram = {
    require(diagram != null && !diagram.isEmpty, "no diagram to render")
    OS.log("Rendering diagram \"" + diagram + "\"")
    DiagramRenderer(cacheDirectory, renderingScript).process(diagram)
  }

  def produce(diagram: String): String = {
    val d = process(diagram)
    val out = json(
      if (d.log.isEmpty) {
        Map(
          "id" -> d.id,
          "source" -> quote(d.source),
          "version" -> version
        )
      } else {
        errorLog(d.log)
      }
    )
    out
  }

}