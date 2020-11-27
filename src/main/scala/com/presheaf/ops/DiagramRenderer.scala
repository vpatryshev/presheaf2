package com.presheaf.ops

import java.io.{ File, FileOutputStream }

import PresheafOps._

import scala.io.Source
import scala.util.{ Failure, Success, Try }

/**
 * xypic Diagram Renderer
 * Produces a pdf and a png file
 */
case class DiagramRenderer(cache: File, script: String, logger: ILog) extends TheyLog {
  require(cache.exists, "Server error, cache directory missing " + cache.getAbsolutePath)
  require(cache.isDirectory, "Server error, check cache directory " + cache.getAbsolutePath)

  def wrap(what: Any, fmt: String): String =
    if (what.toString.isEmpty) "" else fmt.format(what)

  def explain(action: String, results: (Int, String, String)): String = {
    val (code, log, err) = results
    val allLines = code match {
      case 0 => Nil
      case _ =>
        wrap(action, "<code>>%s</code>") ::
          wrap(code, "result = %d") ::
          //        wrap(log, "%s")::
          wrap(err, "<font color='red'>%s</font>") ::
          Nil
    }
    val html = wrap(allLines filter (_.nonEmpty) mkString "</p>\n<p>", "<p>%s</p>")
    html
  }

  def runM(action: String): Try[String] = Try {
    val results = OS.run(action)
    val log = explain(action, results)
    if (results._1 != 0) throw new RuntimeException(log)
    log
  }

  def withExtension(file: File, extension: String): File = {
    val stripped = file.getAbsolutePath.substring(0, file.getAbsolutePath.lastIndexOf('.'))
    new File(stripped + "." + extension)
  }

  def delete(file: File, extensions: List[String]) {
    for (x <- extensions) {
      withExtension(file, x).delete
    }
  }

  def idFor(tex: String): String = "d" + md5(tex)

  def diagramFile(name: String): File = {
    new File(cache, name)
  }

  def process(source: String): Diagram = {
    info("decoded '" + source + "' to '" + source + "'")
    val id = idFor(source)
    val img: File = diagramFile("$id.png")
    val pdf: File = diagramFile("$id.pdf")
    val diagram = new Diagram(id, source, img, pdf)
    val result =
      if (diagram.inCache) diagram
      else doWithScript(diagram)
    info(s"Renderer.process: $result.")
    result
  }

  def upsert(file: File, content: String): Try[Any] = {
    val src: Source = Source.fromFile(file)
    
    val result = Try {
      src.getLines.mkString("\n")
    } filter (content ==) orElse Try {
      val out = new FileOutputStream(file)
      out.write(content.getBytes)
      out.close()
    }
    src.close()
    result
  }

  def doWithScript(diagram: Diagram): Diagram = {
    val id = diagram.id
    val source = diagram.source
    val src = diagramFile(s"$id.src")
    info(s"Will have to do with script: $src")
    upsert(src, source) match {
      case Failure(oops) =>
        error(s"Got an $oops while trying to write to $src - $source")
        diagram
      case Success(_) =>
        runM(s"sh $script $id") match {
          case Success(log) =>
            debug(s"\n------OK-------\n$log")
            diagram
          case Failure(err) =>
            val log = err.getMessage
            debug(s"\n------OOPS -------\n$log")
            diagram.copy(log = log)
        }
    }
  }

}
