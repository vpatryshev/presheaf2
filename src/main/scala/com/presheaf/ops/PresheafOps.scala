package com.presheaf.ops

import java.io._

import com.presheaf.http.Storage._

/**
 * Presheaf operations
 */

trait PresheafOps extends TheyLog {
  val renderingScript: String = findScript(
    "presheaf.sh",
    s"${OS.homeDir}/presheaf.sh"
  ).getAbsolutePath

  def findScript(alternatives: String*): File = {
    alternatives map (new File(_)) find (_.canExecute) getOrElse {
      throw new IllegalStateException(s"Could not find a script among $alternatives")
    }
  }

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
    info(fullLog)
    fullLog match {
      case xyError(msg) =>
        Map(
          "error" -> msg,
          "version" -> BuildInfo.version
        )
      case _ =>
        Map(
          "error" -> fullLog,
          "version" -> BuildInfo.version
        )

    }
  }

  lazy val renderer: DiagramRenderer = DiagramRenderer(cacheDir, renderingScript, logger)

  def process(diagram: String): Diagram = {
    require(diagram != null && !diagram.isEmpty, "no diagram to render")
    info(s"""Rendering diagram "$diagram"""")
    renderer.process(diagram)
  }

  def produce(diagram: String): String = {
    val d = process(diagram)
    val out = json(
      if (d.log.isEmpty) {
        Map(
          "id" -> d.id,
          "source" -> quote(d.source),
          "version" -> BuildInfo.version
        )
      } else {
        errorLog(d.log)
      }
    )
    out
  }

}

object PresheafOps {
  import java.security._

  private val digest = MessageDigest.getInstance("MD5")
  digest.reset()

  def encode(b: Byte): String = java.lang.Integer.toString(b & 0xff, 36)

  def md5(message: String): String = {
    ("" /: digest.digest(message.getBytes("UTF-8")))(_ + encode(_))
  }

}