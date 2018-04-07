package org.presheaf.http

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import StatusCodes.SeeOther

import scala.concurrent.duration._
import org.presheaf.ops.OS._

trait Dispatch {
  val cacheDir: File = new File(homeDir, "cache")
  
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Dispatch])

  // Required by the `ask` (?) method below
  implicit lazy val timeout: Timeout = Timeout(10.seconds) // usually we'd obtain the timeout from the system's configuration

  val static: Route =
    (get & path("")){
      (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(SeeOther)) {
        getFromResource("static/index.html")
      } ~ {
        getFromResourceDirectory("static")
      }
    }  
  
  val cache: Route = (get & pathPrefix("cache")) {
    getFromDirectory(cacheDir.getAbsolutePath)
  }

  val webService: Route = (get & path("dws")) {
    parameter("op") { 
      op =>
        if (op == "samples") {
          println(s"OK, we got an operation <<$op>>")
          getFromResource("samples.txt")
        } else {
          complete((StatusCodes.BadRequest, s"Unknown op <<$op>>"))
        }
    } ~
    parameter("format", "in") { (format, in) =>
      println(s"OK, we got format=$format, in=$in")
      complete((StatusCodes.OK, s"OK, we got format=$format, in=$in"))
    }
  }

  lazy val routes: Route = cache ~ webService ~ static
}
