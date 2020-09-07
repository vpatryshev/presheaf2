package com.presheaf.http

import java.io.{File, FileOutputStream, FileWriter, InputStream}
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.{KeyStore, SecureRandom}
import java.util.Date

import javax.net.ssl.{KeyManagerFactory, SSLContext, SSLParameters, TrustManagerFactory}
import com.typesafe.sslconfig.akka.AkkaSSLConfig

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import de.heikoseeberger.accessus.Accessus._
import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, TLSClientAuth}

import scala.util.{Failure, Success}

//#main-class
object Server extends App with Dispatch {

  createWebroot()
  lazy val flag = new File("presheaf2.flag")

  lazy val HttpPort: Int = Option(args).flatMap(_.headOption).getOrElse("8721").toInt
  lazy val HttpsPort: Int = Option(args).flatMap(_.tail.headOption).getOrElse("8714").toInt
  implicit lazy val system: ActorSystem = ActorSystem("Presheaf")

  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
  
  def run(): Unit = {
    HttpServer(HttpPort, routes, logger).run()
    HttpsServer(HttpsPort, routes, logger).run()
    
    val flagFile = new FileWriter(flag)
    flagFile.write("started " + new Date())
    flagFile.close()
  }

  def stop(): Option[String] = {
    if (flag.exists()) {
      None
    } else {
      info("Shutting down...")
      system.terminate()
      Some("Shutting down")
    }
  }

  run()
  
  Await.result(system.whenTerminated, Duration.Inf)

  lazy val WEBROOT = new File("webroot")
  lazy val WELL_KNOWN = new File(WEBROOT, ".well_known")

  def createWebroot(): Unit = {
    if (!WELL_KNOWN.exists) WELL_KNOWN.mkdirs
    Files.setPosixFilePermissions(WEBROOT.toPath, PosixFilePermissions.fromString("rwxrwxrwx"))
    Files.setPosixFilePermissions(WELL_KNOWN.toPath, PosixFilePermissions.fromString("rwxrwxrwx"))
  }

}
