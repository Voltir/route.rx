package routerx

import utest._
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}

object LocalLinkTest extends TestSuite with implicits.Defaults {
  import utest.ExecutionContext.RunNow

  case class UserId(value: Long) extends AnyVal
  object UserId {
    implicit val like = likelib.like[Long,UserId]
    val default = UserId(42)
  }

  case class FakeUser(uid: UserId, name: String)

  case class OtherRandomThing(foo: Int, bar: String)

  sealed trait Screen
  case object FooScreen extends Screen
  case object BarScreen extends Screen
  case object BazScreen extends Screen
  case class ProfileScreen(user: FakeUser) extends Screen
  case class FriendScreen(user: FakeUser) extends Screen
  case class TestLikeScreen(uid: UserId) extends Screen
  case class MultiScreen(thing: OtherRandomThing, user: FakeUser) extends Screen
  case class TestVolatileScreen(user: FakeUser) extends Screen with VolatileLink

  sealed trait NestedScreen extends Screen
  case object DeepScreen extends NestedScreen
  case class OtherDeepScreen(user: FakeUser) extends NestedScreen with VolatileLink

  sealed trait DeeperScreen extends NestedScreen
  case object ReallyDeepScreen extends DeeperScreen

  //url: /user/profile
  sealed trait UserScreen extends Screen
  @fragment("profile") case object UserProfileScreen extends UserScreen

  //url: /account/profile
  sealed trait AccountScreen extends Screen
  @fragment("profile") case object AccountProfileScreen extends AccountScreen

  sealed abstract class AdminScreen(uid: UserId) extends Screen
  @fragment("profile") case class AdminProfile(uid: UserId) extends AdminScreen(uid)
  case class AdminDetails(user: FakeUser) extends AdminScreen(user.uid)

  implicit val FakeUserUrlParts = new UrlPart[FakeUser] {
    override val size = 1

    override def toParts(inp: FakeUser) = inp.uid.value.toString :: Nil

    override def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[FakeUser] = parts match {
      case id :: Nil => Future.successful {
        FakeUser(
          UserId(id.toLong),
          "This would normally come from the server"
        )
      }
      case _ => Future.failed(new Throwable("Invalid size for FakeUser URL Parts!"))
    }
  }

  implicit val OtherRandomUrlParts = new UrlPart[OtherRandomThing] {
    val size = 3

    def toParts(inp: OtherRandomThing) = List(inp.foo.toString,inp.bar,"EXTRA-PART")

    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[OtherRandomThing] =  parts match  {
      case id :: bar :: _ :: Nil => Future.successful(OtherRandomThing(id.toInt,bar))
      case _ => Future.failed(new Throwable("Invalid Shape for FakeUser partial url!"))
    }
  }

  def tests = TestSuite {
    val routes = Router.generate[Screen](FooScreen)

    'linkToBasics {
      routes.goto(BarScreen)
      assert(routes.current.now == BarScreen)
      assert(dom.window.location.pathname == "/bar")
    }

    'linkWithUrlPart {
      routes.goto(ProfileScreen(FakeUser(UserId(42),"Foo Sam")))
      assert(dom.window.location.pathname == "/profile/42")
    }

    'linkWithMultipleParts {
      val screen = MultiScreen(OtherRandomThing(100,"BAR-STRING"),FakeUser(UserId(99),"Another User"))
      routes.goto(screen)
      assert(dom.window.location.pathname == "/multi/100/BAR-STRING/EXTRA-PART/99")
    }

    'nestedTest {
      routes.goto(DeepScreen)
      assert(dom.window.location.pathname == "/nested/deep")
      routes.goto(OtherDeepScreen(FakeUser(UserId(99),"Another User")))
      assert(dom.window.location.pathname == "/nested/other-deep/99")
    }

    'deeperTest {
      routes.goto(ReallyDeepScreen)
      assert(dom.window.location.pathname == "/nested/deeper/really-deep")
    }

    'fragmentOverrideTest {
      routes.goto(UserProfileScreen)
      assert(dom.window.location.pathname == "/user/profile")
      routes.goto(AccountProfileScreen)
      assert(dom.window.location.pathname == "/account/profile")
    }

    'likeTest {
      routes.goto(TestLikeScreen(UserId(3)))
      assert(dom.window.location.pathname == "/test-like/3")
    }

    'parseUrls {
      * - routes.parseUrl("/foo").map { s => assertMatch(s){case FooScreen => }}
      * - routes.parseUrl("/bar").map { s => assertMatch(s){case BarScreen => }}
      * - routes.parseUrl("/baz").map { s => assertMatch(s){case BazScreen => }}
      * - routes.parseUrl("/profile/99").map { s => assertMatch(s){ case ProfileScreen(FakeUser(UserId(99),_)) =>}}
      * - routes.parseUrl("/friend/42").map { s => assertMatch(s){ case FriendScreen(FakeUser(UserId(42),_)) =>}}
      * - routes.parseUrl("/multi/10/MY-RANDOM-BAR/AND-AlSO-WHO/1001").map {
        s => assertMatch(s){case MultiScreen(OtherRandomThing(10,"MY-RANDOM-BAR"),FakeUser(UserId(1001),_)) =>}
      }
      * - routes.parseUrl("/nested/deep").map { s => assertMatch(s){case DeepScreen => }}
      * - routes.parseUrl("/nested/deeper/really-deep").map { s => assertMatch(s){case ReallyDeepScreen => }}
      * - routes.parseUrl("/user/profile").map { s => assertMatch(s){case UserProfileScreen => }}
      * - routes.parseUrl("/account/profile").map { s => assertMatch(s){case AccountProfileScreen => }}
      * - routes.parseUrl("/test-like/555").map { s => assertMatch(s){case TestLikeScreen(UserId(555)) => }}
    }

    'traitAsPrefixTest {
      sealed trait AdminScreen
      case object ThingScreen extends AdminScreen
      val routes: Router[AdminScreen] = Router.generate[AdminScreen](ThingScreen)
      routes.goto(ThingScreen)
      assert(dom.window.location.pathname == "/admin/thing")
    }

    'dynamicPrefixTest {
      val dynamic = "url-prefix"
      sealed trait TestScreen
      case object OtherScreen extends TestScreen
      val rotuesz: Router[TestScreen] = Router.generateWithPrefix[TestScreen](OtherScreen, dynamic)
      rotuesz.goto(OtherScreen)
      assert(dom.window.location.pathname == "/url-prefix/test/other")
      rotuesz.parseUrl("/url-prefix/test/other").map { s =>  assertMatch(s){case OtherScreen => }}
    }
  }
}
