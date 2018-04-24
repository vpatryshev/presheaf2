package com.presheaf.http

import java.io.{ File, FileOutputStream, FileWriter }
import java.util.Date

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import de.heikoseeberger.accessus.Accessus._
import akka.Done
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import sun.util.logging.resources.logging

import scala.util.{ Failure, Success }

//#main-class
object Server extends App with Dispatch {
  val PORT: Int = args.headOption.getOrElse("8721").toInt
  implicit val system: ActorSystem = ActorSystem("Presheaf")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val moreLogging = new RouteOps(routes).withAccessLog(logger.accessLog)

  Http()
    .bindAndHandle(
      moreLogging,
      "0.0.0.0", PORT
    )
    .onComplete {
      case Success(ServerBinding(address)) => info(s"Listening on $address")
      case Failure(cause) => info(s"Can't bind to localhost:$PORT: $cause")
    }

  info(s"Running on port $PORT...")

  val flag = new File("presheaf2.flag")
  val fos = new FileWriter(flag)
  fos.write("started " + new Date())
  fos.close()

  def stop(): Option[String] = {
    if (flag.exists()) {
      None
    } else {
      info("Shutting down...")
      system.terminate()
      Some("Shutting down")
    }
  }

  Await.result(system.whenTerminated, Duration.Inf)

}
