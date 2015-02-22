package locallink

import utest._
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}


object LocalLinkTest extends TestSuite {
  import utest.ExecutionContext.RunNow

  case class FakeUser(id: Int, name: String)

  case class OtherRandomThing(foo: Int, bar: String)

  sealed trait Screen
  case object FooScreen extends Screen
  case object BarScreen extends Screen
  case object BazScreen extends Screen
  case class ProfileScreen(user: FakeUser) extends Screen
  case class FriendScreen(user: FakeUser) extends Screen
  case class MultiScreen(thing: OtherRandomThing, user: FakeUser) extends Screen
  case class TestVolatileScreen(user: FakeUser) extends Screen with VolatileLink

  implicit val FakeUserUrlParts = new UrlPart[FakeUser] {
    override val size = 1

    override def toParts(inp: FakeUser) = List(inp.id.toString)

    override def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[FakeUser] = parts match {
      case id :: Nil => Future(FakeUser(id.toInt,"This would normally come from the server"))
      case _ => Future.failed(new Throwable("Invalid size for FakeUser URL Parts!"))
    }
  }

  implicit val OtherRandomUrlParts = new UrlPart[OtherRandomThing] {
    val size = 3

    def toParts(inp: OtherRandomThing) = List(inp.foo.toString,inp.bar,"EXTRA-PART")

    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[OtherRandomThing] =  parts match  {
      case id :: bar :: _ :: Nil => Future(OtherRandomThing(id.toInt,bar))
      case _ => Future.failed(new Throwable("Invalid Shape for FakeUser partial url!"))
    }
  }

  def tests = TestSuite {
    val routes = Router.generate[Screen](FooScreen)

    'defaultHref {
      assert(routes.current.now == FooScreen)
      assert(dom.window.location.pathname == "/foo")
     }

    'linkToBasics {
      routes.goto(BarScreen)
      assert(routes.current.now == BarScreen)
      assert(dom.window.location.pathname == "/bar")
    }

    'linkWithUrlPart {
      routes.goto(ProfileScreen(FakeUser(42,"Foo Sam")))
      assert(dom.window.location.pathname == "/profile/42")
    }

    'linkWithMultipleParts {
      val screen = MultiScreen(OtherRandomThing(100,"BAR-STRING"),FakeUser(99,"Another User"))
      routes.goto(screen)
      assert(dom.window.location.pathname == "/multi/100/BAR-STRING/EXTRA-PART/99")
    }

    'parseUrls {
      * - routes.parseUrl("/foo").map { s => assertMatch(s){case FooScreen => }}
      * - routes.parseUrl("/bar").map { s => assertMatch(s){case BarScreen => }}
      * - routes.parseUrl("/baz").map { s => assertMatch(s){case BazScreen => }}
      * - routes.parseUrl("/profile/99").map { s => assertMatch(s){ case ProfileScreen(FakeUser(99,_)) =>}}
      * - routes.parseUrl("/friend/42").map { s => assertMatch(s){ case FriendScreen(FakeUser(42,_)) =>}}
      * - routes.parseUrl("/multi/10/MY-RANDOM-BAR/AND-AlSO-WHO/1001").map {
        s => assertMatch(s){case MultiScreen(OtherRandomThing(10,"MY-RANDOM-BAR"),FakeUser(1001,_)) =>}
      }
    }

    'prefixTest {
      sealed trait AdminScreen
      case object TestScreen extends AdminScreen
      val routes: Router[AdminScreen] = Router.generate[AdminScreen](TestScreen)
      println(dom.window.location.pathname)
      assert(dom.window.location.pathname == "/admin/test")
    }
  }
}
