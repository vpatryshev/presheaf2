package com.presheaf.http

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import de.heikoseeberger.accessus.Accessus._
import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import sun.util.logging.resources.logging

import scala.util.{Failure, Success}

//#main-class
object Server extends App with Dispatch {
  val PORT: Int = 8721
  implicit val system: ActorSystem = ActorSystem("Presheaf")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

//  import akka.http.scaladsl.server.directives.DebuggingDirectives
//
//  val clientRouteLogged =
//    DebuggingDirectives.logRequestResult("Client ReST", Logging.InfoLevel)(routes)

  val moreLogging = new RouteOps(routes).withAccessLog(logger.accessLog)

  Http()
    .bindAndHandle(
      moreLogging,
      "localhost", PORT
    )
    .onComplete {
      case Success(ServerBinding(address)) => info(s"Listening on $address")
      case Failure(cause) => info(s"Can't bind to localhost:$PORT: $cause")
    }

  info(s"Running on port $PORT...")

  Await.result(system.whenTerminated, Duration.Inf)
}
