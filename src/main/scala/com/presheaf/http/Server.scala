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
  lazy val sslConfig = AkkaSSLConfig(system)

  lazy val ks: KeyStore = KeyStore.getInstance("PKCS12")
  lazy val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")

  lazy val password: Array[Char] = "akka-https".toCharArray // do not store passwords in code, read them from somewhere safe!

  def ksload(): Unit = {
    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)
  }

  lazy val keyManagerFactory: KeyManagerFactory =
    {
      ksload()
      val kmf: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      kmf.init(ks, password)
      kmf
    }

  lazy val tmf: TrustManagerFactory = {
    val t = TrustManagerFactory.getInstance("SunX509")
    t.init(ks)
    t
  }

  lazy val sslContext: SSLContext = {
    val instance = SSLContext.getInstance("TLS")
    instance.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    instance
  }

  lazy val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
  lazy val handler: Flow[HttpRequest, HttpResponse, Any] = routes

  import system.dispatcher

  lazy val moreLogging = new RouteOps(routes).withAccessLog(logger.accessLog)

  def runHttp(): Unit = {
    Http()
      .bindAndHandle(
        moreLogging,
        "0.0.0.0", HttpPort
      )
      .onComplete {
        case Success(ServerBinding(address)) => info(s"Listening on $address")
        case Failure(cause) => info(s"Can't bind to localhost:$HttpPort: $cause")
      }

    info(s"Running on port $HttpPort...")

    val fos = new FileWriter(flag)
    fos.write("started " + new Date())
    fos.close()
  }
  
  def runHttps(): Unit = {
    Http().
      bindAndHandle(Server.handler, "0.0.0.0", HttpsPort, connectionContext = Server.https)(Server.materializer)

    println(s"Server is now online at https://localhost:$HttpsPort/demo\nPress RETURN to stop...")

  }
  
  def run(): Unit = {
    runHttp()
    runHttps()
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
