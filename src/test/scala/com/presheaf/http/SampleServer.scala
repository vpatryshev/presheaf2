package com.presheaf.http

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Random

object SampleServer {

  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  val numbers = Source.fromIterator(() =>
    Iterator.continually(Random.nextInt()))

  var orders: List[Item] = Nil

  case class Bid(userId: String, offer: Int)
  case object GetBids
  case class Bids(bids: List[Bid])

  class Auction extends Actor with ActorLogging {
    var bids = List.empty[Bid]
    def receive = {
      case bid @ Bid(userId, offer) =>
        bids = bids :+ bid
        log.info(s"Bid complete: $userId, $offer")
      case GetBids => sender() ! Bids(bids)
      case x => log.info(s"Invalid message <<$x>>")
    }
  }
  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)

  final case class Item(name: String, id: Long)
  final case class Order(items: List[Item])

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)

  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(o => o.id == itemId)
  }
  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) => items ::: orders
      case _ => orders
    }
    Future { Done }
  }

  def main(args: Array[String]) {

    val auction = system.actorOf(Props[Auction], "auction")

    val route: Route =
      // http://localhost:8080/auction
      path("auction") {
        put {
          // curl - X PUT 'http://localhost:8080/auction?bid=17&user=you'
          parameter("bid".as[Int], "user") { (bid, user) =>
            // place a bid, fire-and-forget
            auction ! Bid(user, bid)
            complete((StatusCodes.Accepted, "bid placed"))
          }
        } ~
          get {
            implicit val timeout: Timeout = 5.seconds

            // query the actor for the current auction state
            val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
            complete(bids)
          }
      } ~
        path("hello") {
          // http://localhost:8080/hello
          get {
            complete(HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              "<h1>Say hello to akka-http</h1>"
            ))
          }
        } ~
        path("random") {
          // http://localhost:8080/random
          get {
            complete(HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              numbers.map(n => ByteString(s"$n\n"))
            ))
          }
        } ~
        get {
          // http://localhost:8080/item/42
          pathPrefix("item" / LongNumber) { id =>
            // there might be no item for a given id
            val maybeItem: Future[Option[Item]] = fetchItem(id)

            onSuccess(maybeItem) {
              case Some(item) => complete(item)
              case None => complete(StatusCodes.NotFound)
            }
          }
        } ~
        post {
          // curl - H "Content-Type: application/json" - X POST - d '{"items":[{"name":"xyz","id":42}]}' http://localhost:8080/create-order
          path("create-order") {
            entity(as[Order]) { order =>
              val saved: Future[Done] = saveOrder(order)
              onComplete(saved) { done =>
                complete("order created")
              }
            }
          }
        }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

  }
}