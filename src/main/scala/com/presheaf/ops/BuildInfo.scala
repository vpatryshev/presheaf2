package com.presheaf.ops

object BuildInfo {
  val BuildFile: String = "/buildno.txt"

  def buildNo: String = try {
    val vs = Res.read(BuildFile).getLines().toList
    vs match {
      case Nil => " No build data"
      case v :: Nil => v + "(no build date)"
      case v :: d :: _ => "0000".substring(4 - v.length) + v + " " + d
    }
  } catch {
    case x: Exception => x.getMessage
  }

  def version: String = "2.0.1, build#" + buildNo
}