package com.presheaf.ops

trait ILog {
  def debug(x: => Any): Unit
  def info(x: => Any): Unit
  def error(x: => Any): Unit
  def println(x: => Any): Unit = info(x)
}

trait TheyLog extends ILog {
  def logger: ILog
  def debug(x: => Any): Unit  = logger.debug(x)
  def info(x: => Any): Unit = logger.info(x)
  def error(x: => Any): Unit = logger.error(x)
}

import akka.Done
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Sink
import de.heikoseeberger.accessus.Accessus.AccessLog
import sun.util.logging.resources.logging

import scala.concurrent.Future

case class AkkaLogs(logging: LoggingAdapter) extends ILog {
  private def string(x: Any): String = 
    String.valueOf(x).replaceAllLiterally("\n", "\n    ")

  override def debug(x: => Any): Unit = logging.debug(string(x))

  override def info(x: => Any): Unit = logging.info(string(x))

  override def error(x: => Any): Unit = logging.error(string(x))

  /** Log HTTP method, path, status and response time in micros to the given log at info level. */
  val accessLog: AccessLog[Long, Future[Done]] =
    Sink.foreach {
      case ((req, t0), res) =>
        req.method
        val m = req.method.value
        val p = req.uri.path.toString
        val s = res.status.intValue()
        val t = (now() - t0) / 1000
        //        req.
        info(s"$m $p $s $t")
    }
  private def now() = System.nanoTime()

}