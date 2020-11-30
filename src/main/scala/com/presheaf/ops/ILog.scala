package com.presheaf.ops

import language.postfixOps

trait ILog {
  def debug(x: => Any): Unit
  def info(x: => Any): Unit
  def error(x: => Any): Unit
  def println(x: => Any): Unit = info(x)
}

trait TheyLog extends ILog {
  private lazy val bn = BuildInfo.buildNo.split(" ").head
  def logger: ILog
  def debug(x: => Any): Unit = logger.debug(x)
  def info(x: => Any): Unit = logger.info(s"[#$bn] $x")
  def error(x: => Any): Unit = logger.error(x)
}

object SilentBob extends TheyLog {
  override def logger: ILog = null
  override def debug(x: => Any): Unit = ()
  override def info(x: => Any): Unit = ()
  override def error(x: => Any): Unit = ()
}

import akka.Done
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.headers.`X-Forwarded-For`
import akka.http.scaladsl.model.headers.`X-Real-Ip`
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.stream.scaladsl.Sink
import de.heikoseeberger.accessus.Accessus.AccessLog
import sun.util.logging.resources.logging

import scala.concurrent.Future

case class AkkaLogs(logging: LoggingAdapter) extends ILog {
  private def string(x: Any): String =
    String.valueOf(x).replace("\n", "\n    ")

  override def debug(x: => Any): Unit = logging.debug(string(x))

  override def info(x: => Any): Unit = logging.info(string(x))

  override def error(x: => Any): Unit = logging.error(string(x))

  def clientIp(rq: HttpRequest): Option[RemoteAddress] = {
    rq.headers collect {
      case `X-Forwarded-For`(Seq(address, _*)) ⇒
        address
      case `Remote-Address`(address) ⇒
        address
      case `X-Real-Ip`(address) ⇒
        address
    } headOption
  }

  /** Log HTTP method, path, status and response time in micros to the given log at info level. */
  val accessLog: AccessLog[Long, Future[Done]] =
    Sink.foreach {
      case ((req, t0), res) =>
        val m = req.method.value
        val p = req.uri.path
        val s = res.status.intValue
        val t = (now() - t0) / 1000
        val ip = clientIp(req).getOrElse("(unknown remote)")
        info(s"$ip $m $p $s $t")
    }
  private def now() = System.nanoTime()

}