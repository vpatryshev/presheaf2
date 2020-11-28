package com.presheaf.http

import java.io.{File, FileOutputStream}

import com.presheaf.ops.OS.{homeDir, readFromFile}
import com.presheaf.ops.{History, HistoryRecord}
import spray.json.DefaultJsonProtocol.{jsonFormat3, _}
import spray.json.RootJsonFormat
import spray.json._
import DefaultJsonProtocol._

import scala.io.Source
import scala.util.Try

object Storage {

  val cacheDir: File = List(new File("."), new File(".."), homeDir) map {
    dir => new File(dir, "diagrams").getCanonicalFile
  } find (_.isDirectory) getOrElse {
    throw new IllegalStateException("Could not find diagrams folder")
  }

  val privateDir = new File(cacheDir, "private")
  
  privateDir.mkdir()
  
  def privateCacheDir(id: String): File = {
    new File(privateDir, id)
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

  // formats for unmarshalling and marshalling
  implicit val recordFormat: RootJsonFormat[HistoryRecord] = jsonFormat3(HistoryRecord)
  implicit val historyFormat: RootJsonFormat[History] = jsonFormat1(History)

  def readFor(id: String) : Try[History] = for {
    string <- readFromFile(historyFile(id))
    json <- Try(string.parseJson)
    history <- Try(historyFormat.read(json))
  } yield history
  
  private def historyFile(id: String): File = new File(privateCacheDir(id), "history")
  
  def writeFor(id: String)(history: History): Try[Any] = {
    upsert(historyFile(id), historyFormat.write(history).prettyPrint)
  }

}
