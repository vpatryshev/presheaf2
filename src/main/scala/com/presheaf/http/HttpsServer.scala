package com.presheaf.http

import java.io.InputStream
import java.security.{ KeyStore, SecureRandom }

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ ConnectionContext, Http, HttpsConnectionContext }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.presheaf.ops.{ AkkaLogs, OS }
import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

import scala.util.{ Failure, Success }

case class HttpsServer(port: Int, route: Route, logger: AkkaLogs)(
    implicit
    system: ActorSystem, materializer: ActorMaterializer
) {
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

  def httpsContext: HttpsConnectionContext = ConnectionContext.https(sslContext)

  private def sslContext = {

    lazy val keystore: InputStream =
      OS.streamFromFile("keystore.presheaf.pkcs12").get

    lazy val password: Array[Char] =
      OS.readFromFile("password").get.trim.toCharArray

    lazy val ks: KeyStore = KeyStore.getInstance("PKCS12")
    ks.load(keystore, password)

    lazy val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    sslContext
  }
}
