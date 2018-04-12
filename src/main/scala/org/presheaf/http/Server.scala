package org.presheaf.http

//#quick-start-server
import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

//#main-class
object Server extends App with Dispatch {
  implicit val system: ActorSystem = ActorSystem("PresheafTheater")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  //  Http().bindAndHandle(routes, "localhost", 8080)

  import akka.http.scaladsl.server.directives.DebuggingDirectives

  val clientRouteLogged = DebuggingDirectives.logRequestResult("Client ReST", Logging.InfoLevel)(routes)
  Http().bindAndHandle(clientRouteLogged, "localhost", 8080)

  println(s"Running...")

  Await.result(system.whenTerminated, Duration.Inf)
}
