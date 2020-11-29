package com.presheaf.http

//#user-routes-spec
//#test-top
import java.io.File
import java.net.URLEncoder

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{ StatusCodes, _ }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.presheaf.ops.{ OS, Res, _ }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }
import spray.json.DefaultJsonProtocol._
import Storage._

// @see https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/marshalling-directives/entity.html
class DispatchTest extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with Dispatch {

  val version = "2.0.0, build#0042 Fri Nov 27 12:31:15 PST 2020"

  setLogging("off")

  def stop(): Option[String] = None

  private val testFileName: String = "testFile" + System.currentTimeMillis + ".txt"
  val testFile = new File(cacheDir, testFileName)

  val renderingScriptFileName: String = s"$cacheDir/testRenderer.sh"
  val renderingScript: String =
    """
      |
    """.stripMargin
  OS.writeTo(new File(renderingScriptFileName), renderingScript)

  override def afterAll(): Unit = {
    super.afterAll()
    testFile.delete()
  }

  "Dispatch" should {

    "return index.html" in {
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
    "return examples" in {
      val request = HttpRequest(uri = "/dws?op=samples")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        entityAs[String].take(69) should ===(
          """[{"id":"d3e436z2t5w3p603o2v6838s494d1r32","source":"\\n& \\\\lambda\\"""
        )
      }
    }
    "produce diagrams" in {
      val diagramSource = "U \\ar@/_/[ddr]_y \\ar@/^/[drr]^x\n  \\ar@{.>}[dr]|-{(x,y)} \\\\\n  & X \\times_Z Y \\ar[d]^q \\ar[r]_p & X \\ar[d]_f  \\\\\n  & Y \\ar[r]^g & Z"
      val diagramEncoded = URLEncoder.encode(diagramSource, "UTF-8")

      val request = HttpRequest(uri = s"/dws?in=$diagramEncoded")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should ===(
          s"""{"id":"d1t692m6s575z36353t6q4w294y69696s","source":"U \\\\\\\\ar@/_/[ddr]_y \\\\\\\\ar@/^/[drr]^x\\\\n  \\\\\\\\ar@{.>}[dr]|-{(x,y)} \\\\\\\\\\\\\\\\\\\\n  & X \\\\\\\\times_Z Y \\\\\\\\ar[d]^q \\\\\\\\ar[r]_p & X \\\\\\\\ar[d]_f  \\\\\\\\\\\\\\\\\\\\n  & Y \\\\\\\\ar[r]^g & Z\","version":"$version"}"""
        )
      }
    }
    "return a file from cache" in {
      val text = s"Once (${new java.util.Date} upon a midnight dreary (midday eerie)\n"
      OS.writeTo(testFile, text)
      val request = HttpRequest(uri = s"/cache/$testFileName")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should ===(text)
      }
    }

    //#testing-post
    "be able to receive history (POST /history) without cookies" in {
      val entry1 = HistoryRecord("Kapi", 42, None)
      val historyToSend = Map("this_is_an_id" -> entry1)
      val historyEntity: MessageEntity = Marshal(historyToSend).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/history").withEntity(historyEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        // TEMPORARY!        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"entries":{"this_is_an_id":{"text":"Kapi","date":42}}}""")
      }
    }
    //#testing-post

  }
  //#testing-post
  "be able to receive history (POST /history) with cookies" in {
    val entry1 = HistoryRecord("Kapi", 42, None)
    val historyToSend = Map("this_is_an_id" -> entry1)
    val historyEntity: MessageEntity = Marshal(historyToSend).to[MessageEntity].futureValue // futureValue is from ScalaFutures

    // using the RequestBuilding DSL:
    val request = Post("/history").withEntity(historyEntity)
    //

    request ~> Cookie("id" -> "4087688721", "trash" -> "dumpster") ~> routes ~> check {
      status should ===(StatusCodes.OK)
      responseAs[String] shouldEqual """{"entries":{"this_is_an_id":{"text":"Kapi","date":42}}}"""
      // we expect the response to be json:
      // TEMPORARY!        contentType should ===(ContentTypes.`application/json`)

      // and we know what message we're expecting back:
      entityAs[String] should ===("""{"entries":{"this_is_an_id":{"text":"Kapi","date":42}}}""")
    }
  }
  //#testing-post
}
