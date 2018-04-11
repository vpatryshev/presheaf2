package org.presheaf.ops

import java.io.{IOException, File, FileOutputStream}
import OS._

/**
 * xypic Diagram Renderer
 * Produces a pdf and a png file
 */
case class DiagramRenderer(cache: File, script: String = "$homeDir/presheaf.sh") {
  require(cache.exists, "Server error, cache directory missing " + cache.getAbsolutePath)
  require(cache.isDirectory, "Server error, check cache directory " + cache.getAbsolutePath)

  def wrap(what: Any, fmt: String): String =
    if (what.toString.isEmpty) "" else fmt.format(what)
  
  def explain(action: String, results: (Int, String, String)): String = {
    val (code, log, err) = results
    val allLines = code match {
      case 0 => Nil
      case _       => 
        wrap(action, "<code>>%s</code>")::
        wrap(code, "result = %d")::
//        wrap(log, "%s")::
        wrap(err, "<font color='red'>%s</font>")::
        Nil
    }
    val html = wrap(allLines filter(_.nonEmpty) mkString("</p>\n<p>"), "<p>%s</p>")
    html
  }

  def runM(action: String): (Int, String) = {
    val results = OS.run(action)
    val log = explain(action, results)
    (results._1, log)
  }

  def withExtension(file: File, extension: String): File = {
    val stripped = file.getAbsolutePath.substring(0, file.getAbsolutePath.lastIndexOf('.'))
    new File(stripped + "." + extension)
  }

  def delete(file: File, extensions: List[String]) {
    for (x <- extensions) { withExtension(file, x).delete }
  }

  def idFor(tex: String): String = "d" + DiagramRenderer.md5(tex)

  def diagramFile(name: String): File = {
    new File(cache, name)
  }

  def process(source: String) : Diagram = {
      OS.log("decoded '" + source + "' to '" + source + "'")
      val id = idFor(source)
      val img: File = diagramFile("$id.png")
      val pdf: File = diagramFile("$id.pdf")
      val diagram = new Diagram(id, source, img, pdf)
      val result =
          if (diagram.inCache) diagram
          else             doWithScript(diagram)
      OS.log(s"Renderer.process: $result.")
      result
  }

  def doWithScript(diagram: Diagram): Diagram = {
    val id = diagram.id
    val source = diagram.source
    val src = diagramFile(s"$id.src")
    try {
      val srcFile = new FileOutputStream(src)
      srcFile.write(source.getBytes)
      srcFile.close()
      println(s"got $source")
    } catch {
      case x: Exception => 
        println(s"Got an $x while trying to write to $src - $source")
    }

    val command  = s"sh $script $id"
    runM(command) match {
      case (0, _) =>
        println("\n------OK-------")
        diagram
      case (otherwise, log) =>
        println(s"\n------OOPS $otherwise-------\n$log")
        diagram.copy(log = log)
    }
  }
}

object DiagramRenderer {

  import java.security._
  private val digest = MessageDigest.getInstance("MD5")
  digest.reset()
  def encode(b: Byte): String = java.lang.Integer.toString(b & 0xff, 36)

  def md5(message: String): String = {
    ("" /: digest.digest(message.getBytes("UTF-8"))) (_+ encode(_))
  }

}
