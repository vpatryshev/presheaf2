package org.presheaf.http

//#user-routes-spec
//#test-top
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.presheaf.ops.Res
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

//#set-up
class DispatchTest extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with Dispatch {

  "Dispatch" should {
    "return index.thml" in {
      val request = HttpRequest(uri = "/")
      
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
        val indexHtml = Res.string("/static/index.html")

        entityAs[String] should ===(indexHtml)
      }
    }
    "return privacy.html" in {
      val request = HttpRequest(uri = "/static/privacy.html")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`text/html(UTF-8)`)
        val indexHtml = Res.string("/static/privacy.html")
        entityAs[String] should ===(indexHtml)
      }
    }
    "return examples if asked" in {
      val request = HttpRequest(uri = "/dws?op=samples")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        entityAs[String].take(69) should ===(
          """[{"id":"d3e436z2t5w3p603o2v6838s494d1r32","source":"\\n& \\\\lambda\\""")
      }
    }
  }
}
