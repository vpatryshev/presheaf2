package org.presheaf.http

import java.io.File

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.presheaf.ops.{ DiagramSamples, PresheafOps }

import org.presheaf.ops.OS._

trait Dispatch {
  val cacheDir: File = new File(homeDir, "cache")

  private val ops = new PresheafOps {
    val cacheDirectory: File = cacheDir
  }

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Dispatch])

  //  // Required by the `ask` (?) method below
  //  implicit lazy val timeout: Timeout = Timeout(10.seconds) // usually we'd obtain the timeout from the system's configuration

  val staticFiles: Route =
    (get & path(""))(getFromResource("static/index.html")) ~
      (get & path("favicon.ico"))(getFromResource("static/favicon.ico")) ~
      (get & pathPrefix("static"))(getFromResourceDirectory("static"))

  val cachedFiles: Route =
    (get & pathPrefix("cache"))(getFromDirectory(cacheDir.getAbsolutePath))

  val service: Route = (get & path("dws")) {
    parameter("op") {
      case "samples" =>
        println(s"OK, serving samples")
        val rendered = DiagramSamples.samples map ops.produce
        complete(StatusCodes.OK, rendered.mkString("[", ",\n", "]"))

      case somethingWrong =>
        complete((StatusCodes.BadRequest, s"Unknown op <<$somethingWrong>>"))
    } ~ parameter("in") { in =>
      println(s"OK, request in=$in")
      complete((StatusCodes.OK, ops.produce(in)))
    }
  }

  lazy val routes: Route = staticFiles ~ cachedFiles ~ service

}
