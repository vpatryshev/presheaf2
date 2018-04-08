package org.presheaf.ops

import java.io.{IOException, File, FileOutputStream}
import OS._

/**
 * xypic Diagram Renderer
 * Produces a pdf and a png file
 */
case class DiagramRenderer(cache: File) {
  require(cache.exists, "Server error, cache directory missing " + cache.getAbsolutePath)
  require(cache.isDirectory, "Server error, check cache directory " + cache.getAbsolutePath)

  def wrap(what: Any, fmt: String): String =
    if (what.toString.isEmpty) "" else fmt.format(what)
  
  def explain(action: String, results: (Int, String, String)): String = {
    val (code, log, err) = results
    val allLines = code match {
      case 0 => Nil
      case _       => 
//        wrap(action, "<code>>%s</code>")::
//        wrap(code, "result = %d")::
        wrap(log, "%s")::
        wrap(err, "<font color='red'>%s</font>")::
        Nil
    }
    val html = allLines filter(_.nonEmpty) mkString("<p>", "</p>\n<p>", "</p>")
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
    new File(cache, name + ".tex")
  }

  def process(sourceDiagram: String) : Diagram = {
      OS.log("decoded '" + sourceDiagram + "' to '" + sourceDiagram + "'")
      val id = idFor(sourceDiagram)
      val img: File = new File(cache, id + ".png")
      val pdf: File = new File(cache, id + ".pdf")
      val result =
          if (img.exists) new Diagram(id, sourceDiagram, img, pdf)
          else             doWithScript(sourceDiagram, id)
      OS.log(s"Renderer.process: $result.")
      result
  }

  def doWithScript(source: String, name: String): Diagram = {
    val file = diagramFile(name)
    val src: File = withExtension(file, "src")
    try {
      val srcFile = new FileOutputStream(src)
      srcFile.write(source.getBytes)
      srcFile.close()
    } catch {
      case ioe: IOException => 
        println(s"Got an $ioe while trying to write to $src - $source")
//        log = "<p>Diagram already in the system, can't override</p>"
    }
    val img: File = withExtension(file, "png")
    val pdf: File = withExtension(file, "pdf")

    val command  = s"sh $homeDir/doit.sh $name"
    // TODO: figure out wtf I transform an option to a tuple. it's wrong!
    runM(command) match {
      case (0, _) =>
        println("\n------OK-------")
        Diagram(name, source, img, pdf)
      case (otherwise, log) =>
        println(s"\n------OOPS $otherwise-------\n$log")
        Diagram(name, source, img, pdf, log)
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
