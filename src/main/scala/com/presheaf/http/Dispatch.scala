package com.presheaf.http

import java.io.File

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ RemoteAddress, StatusCodes }
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.presheaf.ops.OS._
import com.presheaf.ops.PresheafOps.md5
import com.presheaf.ops._
import spray.json.DefaultJsonProtocol._
import spray.json.{ JsValue, RootJsonFormat }
import Storage._

trait Dispatch extends TheyLog { dispatch =>

  def stop(): Option[String]

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  // formats for unmarshalling and marshalling
  implicit val recordFormat: RootJsonFormat[HistoryRecord] = jsonFormat3(HistoryRecord)
  //  implicit val executionContext = system.dispatcher

  def setLogging(level: String): Unit =
    Logging.levelFor(level).foreach(system.eventStream.setLogLevel)

  lazy val logger: AkkaLogs = AkkaLogs(Logging(system, "FULL_LOG"))

  private val ops: PresheafOps = new PresheafOps {
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
      entity(as[Map[String, HistoryRecord]]) { historyMap =>
        extractClientIP { ip =>
          val ips = ip.toOption.map(_.getHostAddress).getOrElse("unknown")
          val history = History(historyMap)
          optionalCookie("id") {
            case Some(id) => completeWith(ips, history, id.value)
            case None => completeWith(ips, history, ip2id(ips))
          }
        }
      }
    }
  }

  private def completeWith(ip: String, history: History, id: String) = {
    info(s"received:\n$history from  ip=$ip, id=$id")
    val newHistory = history.sync(id)
    setCookie(HttpCookie("id", value = id)) {
      complete(StatusCodes.OK, newHistory)
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
