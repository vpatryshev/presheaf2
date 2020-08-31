package com.presheaf.http

import java.io.{ File, FileOutputStream, FileWriter }
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Date

import javax.net.ssl.{ SSLContext, SSLParameters }
import com.typesafe.sslconfig.akka.AkkaSSLConfig

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import de.heikoseeberger.accessus.Accessus._
import akka.actor.ActorSystem
import akka.http.scaladsl.{ Http, HttpsConnectionContext }
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.{ ActorMaterializer, TLSClientAuth }

import scala.util.{ Failure, Success }

//#main-class
object Server extends App with Dispatch {

  createWebroot()

  val PORT: Int = args.headOption.getOrElse("8721").toInt
  implicit val system: ActorSystem = ActorSystem("Presheaf")
  val sslConfig = AkkaSSLConfig()
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

  lazy val WEBROOT = new File("webroot")
  lazy val WELL_KNOWN = new File(WEBROOT, ".well_known")

  def createWebroot(): Unit = {
    if (!WELL_KNOWN.exists) WELL_KNOWN.mkdirs
    Files.setPosixFilePermissions(WEBROOT.toPath, PosixFilePermissions.fromString("rwxrwxrwx"))
    Files.setPosixFilePermissions(WELL_KNOWN.toPath, PosixFilePermissions.fromString("rwxrwxrwx"))
  }
}
