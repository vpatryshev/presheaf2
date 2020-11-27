package com.presheaf.http

import java.io.File

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.presheaf.ops.OS._
import com.presheaf.ops.PresheafOps.md5
import com.presheaf.ops._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

trait Dispatch extends TheyLog { dispatch =>

  val cacheDir: File = List(new File("."), new File(".."), homeDir) map {
    dir => new File(dir, "diagrams").getCanonicalFile
  } find (_.isDirectory) getOrElse {
    throw new IllegalStateException("Could not find diagrams folder")
  }

  def stop(): Option[String]

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  // formats for unmarshalling and marshalling
  implicit val entryFormat: RootJsonFormat[HistoryEntry] = jsonFormat3(HistoryEntry)
  implicit val historyFormat: RootJsonFormat[History] = jsonFormat1(History)
  //
  //  implicit val executionContext = system.dispatcher

  def setLogging(level: String): Unit =
    Logging.levelFor(level).foreach(system.eventStream.setLogLevel)

  lazy val logger: AkkaLogs = AkkaLogs(Logging(system, "FULL_LOG"))

  private val ops: PresheafOps = new PresheafOps {
    val cacheDirectory: File = cacheDir
    def logger: AkkaLogs = dispatch.logger
  }

  val staticFiles: Route =
    (get & path(""))(getFromResource("static/index.html")) ~
      (get & path("robots.txt"))(getFromResource("static/robots.txt")) ~
      (get & path("favicon.ico"))(getFromResource("static/favicon.ico")) ~
      (get & pathPrefix("static"))(getFromResourceDirectory("static")) ~
      get(getFromBrowseableDirectories("webroot"))

  val cachedFiles: Route =
    (get & pathPrefix("cache"))(getFromDirectory(cacheDir.getAbsolutePath))

  val service: Route = (get & path("dws")) {
    parameter("op") {
      case "samples" =>
        val rendered = DiagramSamples.samples map ops.produce
        complete(StatusCodes.OK, rendered.mkString("[", ",\n", "]"))

      case "ostanovite" =>
        stop() match {
          case Some(text) => complete(StatusCodes.OK, text)
          case None => complete(StatusCodes.NotFound, "yeah right hacker")
        }

      case somethingWrong =>
        complete((StatusCodes.BadRequest, s"Unknown op <<$somethingWrong>>"))
    } ~ parameter("in") { in =>
      complete((StatusCodes.OK, ops.produce(in)))
    }
  } ~ post {
    // curl - H "Content-Type: application/json" - X POST - d '{"items":[{"name":"xyz","id":42}]}' http://localhost:8080/create-order
    path("history") {
      entity(as[Map[String, HistoryEntry]]) { history =>
        optionalCookie("id") {
          case Some(id) =>
            info(s"received from $id:\n$history")
            complete(StatusCodes.OK, s"got ${history.size} record(s) from $id")
          case None =>
            extractClientIP { ip =>
              val ips = ip.toOption.map(_.getHostAddress).getOrElse("unknown")
              info(s"received:\n$history from  ip=$ips from $ip")
              setCookie(HttpCookie("id", value = ip2id(ips))) {
                complete(StatusCodes.OK, s"got ${history.size} record(s)")
              }
            }
        }
      }
    }
  }

  lazy val routes: Route = staticFiles ~ cachedFiles ~ service

  /**
   * Create a user id based on use's ip
   * @param ip user's ip (or "unknown")
   * @return an id; starts with '0' which is a prefix for this kind of id.
   */
  def ip2id(ip: String): String = "0" + md5(ip)
}
