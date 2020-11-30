package com.presheaf.http

import java.io.{ File, FileOutputStream }

import com.presheaf.ops.OS.{ homeDir, readFromFile }
import com.presheaf.ops.{ History, HistoryRecord, SilentBob, TheyLog }
import spray.json.DefaultJsonProtocol.{ jsonFormat3, _ }
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

  def upsert(file: File, content: String)(implicit log: TheyLog = SilentBob): Try[Any] = {
    val original: Try[String] = Try {
      Source.fromFile(file)
    } flatMap { src =>
      val result = Try {
        src.getLines.mkString("\n")
      }
      src.close()
      result
    }
    log.info(s"upsert($file): read: ${original.isSuccess}")
    original filter (content ==) orElse Try {
      file.getParentFile.mkdirs()
      val out = new FileOutputStream(file)
      out.write(content.getBytes)
      out.close()
      log.info(s"written ${content.length} bytes to $file")
    }
  }

  // formats for unmarshalling and marshalling
  implicit val recordFormat: RootJsonFormat[HistoryRecord] = jsonFormat3(HistoryRecord)
  implicit val historyFormat: RootJsonFormat[History] = jsonFormat1(History)

  def readFor(id: String)(implicit log: TheyLog = SilentBob): Try[History] = for {
    string <- readFromFile(historyFile(id))
    _ = log.info(s"reading history ${string.length} bytes from ${historyFile(id)}")
    json <- Try(string.parseJson)
    _ = log.info(s"json ok")
    history <- Try(historyFormat.read(json))
    _ = log.info(s"history ok")
  } yield history

  def historyFile(id: String): File = new File(privateCacheDir(id), "history")

  def writeFor(id: String)(history: History)(implicit log: TheyLog = SilentBob): Try[Any] = {
    upsert(historyFile(id), historyFormat.write(history).prettyPrint)(log)
  }

}
