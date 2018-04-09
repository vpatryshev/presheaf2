package org.presheaf.http

//#user-routes-spec
//#test-top
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

//#set-up
class SomeSampleSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with Dispatch {
  //#test-top

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes, 
  // but we could "mock" it by implementing it in-place or by using a TestProbe() 
//  override val userRegistryActor: ActorRef =
//    system.actorOf(UserRegistryActor.props, "userRegistry")

  //#set-up

  //#actual-test
  "Dispatch" should {
    "return examples if asked" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/dws?op=samples")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        // and no entries should be in the list:
        entityAs[String].take(69) should ===(
          """[{"id":"d3e436z2t5w3p603o2v6838s494d1r32","source":"\\n& \\\\lambda\\""")
      }
    }
    //#actual-test

    //#testing-post
//    "be able to add users (POST /users)" in {
//      val user = User("Kapi", 42, "jp")
//      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures
//
//      // using the RequestBuilding DSL:
//      val request = Post("/users").withEntity(userEntity)
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.Created)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and we know what message we're expecting back:
//        entityAs[String] should ===("""{"description":"User Kapi created."}""")
//      }
//    }
    //#testing-post

//    "be able to remove users (DELETE /users)" in {
//      // user the RequestBuilding DSL provided by ScalatestRouteSpec:
//      val request = Delete(uri = "/users/Kapi")
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and no entries should be in the list:
//        entityAs[String] should ===("""{"description":"User Kapi deleted."}""")
//      }
//    }
    //#actual-test
  }
  //#actual-test

  //#set-up
}
//#set-up
//#user-routes-spec
