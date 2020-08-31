package com.presheaf

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ ActorMaterializer, _ }
import akka.util.ByteString
import akka.{ Done, NotUsed }

import scala.concurrent._

// @see https://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M3/scala/stream-cookbook.html#Parsing_lines_from_a_stream_of_ByteStrings

object StreamSamples extends App {
  def K[T, U](t: => T) = (any: U) => t

  implicit class MyFuture[T](f: Future[T]) {
    def >>>(op: T => Unit): Unit = f.onComplete(_.foreach(op))
  }

  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  val factorialPath = Paths.get("factorials.txt")

  private val f1: Future[Done] = {
    val source: Source[Int, NotUsed] = Source(1 to 110)
    source.runForeach(i ⇒ println(i))(materializer)
  }

  private val f2: Future[IOResult] = {
    val factorials = Source(1 to 30).scan(BigInt(1))((acc, next) ⇒ acc * next)

    val result: Future[IOResult] =
      factorials
        .map(num ⇒ ByteString(s"$num\n"))
        .runWith(FileIO.toPath(factorialPath))

    result
  }

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") ⇒ Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }

  def f3: Future[Done] = {
    val akkaTag = Hashtag("#akka")

    val tweets: Source[Tweet, NotUsed] = Source(
      Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
        Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
        Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka ! and #apples") ::
        Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
        Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
        Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
        Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
        Nil
    )

    tweets
      .map(_.hashtags) // Get all sets of hashtags ...
      .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
      .mapConcat(identity) // Flatten the stream of tweets to a stream of hashtags (no)
      .map(_.name.toUpperCase) // Convert all hashtags to upper case
      .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags

  }

  def f4: Future[Done] = {
    val all = FileIO.fromPath(factorialPath)
    val numbersBS = all.via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
    val numbers = numbersBS map (_.utf8String)
    numbers.runWith(Sink.foreach(s => println(s"<<$s>>")))
  }

  (f1 flatMap K(f2) flatMap K(f3) flatMap K(f4)) >>> K(system.terminate())
}
