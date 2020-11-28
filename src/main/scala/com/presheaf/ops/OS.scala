package com.presheaf.ops

import java.io._
import java.util.Locale

import OS.KindOfOS

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.io.Source
import scala.sys.process._
import scala.util.Try
import scala.util.matching.Regex

/**
 * OS - representing Operating System
 */

object OS {

  def run(cmd: String): (Int, String, String) = {
    val se = new StringBuilder
    val so = new StringBuilder
    val is = new ByteArrayInputStream("\4,\4,\4,\4".getBytes)
    val status = cmd #< is ! ProcessLogger(
      o => so append (o + "\n"),
      e => se append (e + "\n")
    )
    (status, so.toString, se.toString)
  }

  def ln(target: File, link: File) {
    run("ln -s " + target.getAbsolutePath + " " + link.getAbsolutePath)
  }

  def chmod(target: File, flags: String) {
    run("chmod " + flags + " " + target.getAbsolutePath)
  }

  def cp(source: File, target: File) {
    run("cp " + source.getAbsolutePath + " " + target.getAbsolutePath)
  }

  def whoami: String = {
    run("whoami")._2.trim
  }

  sealed abstract class KindOfOS(namePattern: String, home: String = "/home/") {
    def matches(name: String): Boolean = name matches namePattern
    def homeRoot: File = new File(home)
    val regCand = s"${home.replaceAll("\\\\", "\\\\")}(\\w+)"
    val IsHome: Regex = s"${home.replaceAll("\\\\", "\\\\\\\\")}(\\w+)".r
  }

  case object Mac extends KindOfOS("(mac|darwin).*", "/Users/")
  case object Windows extends KindOfOS("win.*", "\\Users\\")
  case object Linux extends KindOfOS(".*nux.*")
  case object Unix extends KindOfOS(".*(nix|aix).*")
  case class Unknown(name: String) extends KindOfOS("WTF", "not a legal folder")

  val thisOS: KindOfOS = {
    val name = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
    List(Mac, Windows, Linux, Unix, Unknown("?")) find (_ matches name) getOrElse Unknown(name)
  }

  val homeDir: File = {
    @tailrec
    def findIt(file: File): Option[File] = {
      val ap = file.getAbsolutePath
      ap match {
        case thisOS.IsHome(owner) => Some(file)
        case p if p contains "/" => findIt(file.getParentFile)
        case _ => None
      }
    }

    val curdir = Option(new File(".").getAbsoluteFile)
    val optional = curdir flatMap findIt map (_.getAbsoluteFile)
    optional getOrElse (throw new UnknownError(s"Cannot find my home! Lost in $curdir"))
  }

  def writeTo(file: File, text: String): Try[Unit] = Try {
    val fw = new FileWriter(file)
    fw.write(text)
    fw.close()
  }

  def streamFromFile(path: String): Try[FileInputStream] = Try {
    val file = new File(path)
    require(file.canRead, s"Oops, could not read file  '$file'")
    new FileInputStream(file)
  }

  def readFromFile(path: String): Try[String] = readFromFile(new File(path))

  def readFromFile(file: File): Try[String] = Try {
    require(file.canRead, s"Oops, could not read file  '$file'")
    val source = Source.fromFile(file)
    try source.mkString finally source.close()
  }
}