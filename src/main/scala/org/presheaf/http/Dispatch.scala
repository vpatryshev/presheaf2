package org.presheaf.http

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import org.presheaf.ops.{Diagram, DiagramSamples, PresheafOps}

import scala.concurrent.duration._
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
    (get & path("")) {
      pathEndOrSingleSlash {
        getFromResource("static/index.html")
      }
    } ~ (get & pathPrefix("static")) {
        getFromResourceDirectory("static")
    }  
  
  val cachedFiles: Route = (get & pathPrefix("cache")) {
    getFromDirectory(cacheDir.getAbsolutePath)
  }

  val service: Route = (get & path("dws")) {
    parameter("op") { 
      op =>
        if (op == "samples") {
          println(s"OK, we got an operation <<$op>>")
          val rendered = DiagramSamples.samples map ops.produce
          
          complete(StatusCodes.OK, rendered.mkString("[", ",\n", "]"))

        } else {
          complete((StatusCodes.BadRequest, s"Unknown op <<$op>>"))
        }
    } ~ parameter("format", "in") { (format, in) =>
      println(s"OK, we got format=$format, in=$in")
      val json = ops.produce(in)
      complete((StatusCodes.OK, json))
    }
  }

  lazy val routes: Route = staticFiles ~ cachedFiles ~ service

}
