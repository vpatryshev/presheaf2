package com.presheaf.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.presheaf.ops.AkkaLogs
import de.heikoseeberger.accessus.Accessus._

import scala.util.{Failure, Success}

case class HttpServer(port: Int, route: Route, logger: AkkaLogs)(
  implicit system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  def run(): Unit = {

    lazy val routesWithLogging = new RouteOps(route).withAccessLog(logger.accessLog)
    Http()
      .bindAndHandle(routesWithLogging,"0.0.0.0", port)
      .onComplete {
        case Success(ServerBinding(address)) => logger.info(s"Listening on $address")
        case Failure(cause) => logger.error(s"Can't bind to localhost:$port: $cause")
      }

    logger.info(s"Running HTTP on port $port...")

  }

}
