package com.presheaf.http

//#user-routes-spec
//#test-top
import java.io.{ File, FileWriter }
import java.net.URLEncoder

import akka.actor.ActorRef
import akka.event.Logging
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.presheaf.ops.{ AkkaLogs, OS, Res }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

//#set-up
class DispatchTest extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with Dispatch {

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
          "{\"id\":\"d1t692m6s575z36353t6q4w294y69696s\",\"source\":\"U \\\\\\\\ar@/_/[ddr]_y \\\\\\\\ar@/^/[drr]^x\\\\n  \\\\\\\\ar@{.>}[dr]|-{(x,y)} \\\\\\\\\\\\\\\\\\\\n  & X \\\\\\\\times_Z Y \\\\\\\\ar[d]^q \\\\\\\\ar[r]_p & X \\\\\\\\ar[d]_f  \\\\\\\\\\\\\\\\\\\\n  & Y \\\\\\\\ar[r]^g & Z\",\"version\":\"1.2.0, build#0033 Tue Aug 25 08:18:55 PDT 2020\"}"
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
  }
}
