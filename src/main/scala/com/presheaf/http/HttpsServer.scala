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
import com.presheaf.ops.AkkaLogs

import scala.io.Source
import scala.util.{Failure, Success}

case class HttpsServer(port: Int, route: Route, logger: AkkaLogs)(
  implicit system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  def run(): Unit = {
    val https: HttpsConnectionContext = httpsContext
    val handler: Flow[HttpRequest, HttpResponse, Any] = route
    
    Http().bindAndHandle(handler, "0.0.0.0", port, httpsContext)
      .onComplete {
        case Success(ServerBinding(address)) => logger.info(s"Listening on $address")
        case Failure(cause) => logger.info(s"Can't bind to localhost:$port: $cause")
      }

    logger.info(s"Running HTTPS on port $port...")
  }

  def httpsContext: HttpsConnectionContext = {
    lazy val sslConfig = AkkaSSLConfig(system)

    lazy val ks: KeyStore = KeyStore.getInstance("PKCS12")
  lazy val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.presheaf.pkcs12")
    require(keystore != null, "Keystore required!")

  lazy val password: Array[Char] = {
    val file = new File("password")
    require(file.canRead, "Oops, could not find 'password' file")
    val pwd = Source.fromFile(file).mkString.trim
    pwd.toCharArray
  }

    ks.load(keystore, password)

    lazy val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")

    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)
    https
  }

}
