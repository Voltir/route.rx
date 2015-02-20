package locallink

import utest._
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}


object LocalLinkTest extends TestSuite {
  import scalajs.concurrent.JSExecutionContext.Implicits.queue

  case class FakeUser(id: Int, name: String)

  case class OtherRandomThing(foo: Int, bar: String)

  sealed trait Screen
  case object FooScreen extends Screen
  case object BarScreen extends Screen
  case object BazScreen extends Screen
  case class ProfileScreen(user: FakeUser) extends Screen
  case class FriendScreen(user: FakeUser) extends Screen
  case class MagicScreen(thing: OtherRandomThing, user: FakeUser) extends Screen

  implicit val LolDeathToAndrew = new UrlPartial[FakeUser] {

    val numParts = 1

    def toParts(inp: FakeUser) = List(inp.id.toString)

    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[FakeUser] = {
      parts match {
        case id :: Nil => Future(FakeUser(id.toInt,"Should pull From Server"))
        case _ => Future.failed(new Throwable("Invalid Shape for FakeUser partial url!"))
      }
    }
  }

  implicit val LolDeathToAndrew2 = new UrlPartial[OtherRandomThing] {

    val numParts = 3

    def toParts(inp: OtherRandomThing) = List(inp.foo.toString,inp.bar,"AND-AlSO-WHO")

    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[OtherRandomThing] = {
      parts match {
        case id :: bar :: _ :: Nil => Future(OtherRandomThing(id.toInt,bar))
        case _ => Future.failed(new Throwable("Invalid Shape for FakeUser partial url!"))
      }
    }
  }

  def tests = TestSuite {
    'test1 {
      println("~~~~~~~ AWHAAAT AM I DOING??? ~~~~~~~~~")
      val thing = OtherRandomThing(10,"MY-RANDOM-THING-NAME")
      val routes = Router.generate[Screen](FooScreen)
      def printCurrent() = {
        println("~~~~~~~~~~~~~~~~~~~~~~~~~")
        println(routes.current.now)
        println(dom.window.location.href)
      }
      printCurrent()
      routes.linkTo(ProfileScreen(FakeUser(2,"Foo Sam")))
      printCurrent()
      routes.linkTo(BarScreen)
      printCurrent()
      routes.linkTo(ProfileScreen(FakeUser(42,"Biz Bob Sam")))
      printCurrent()
      routes.linkTo(FriendScreen(FakeUser(42,"Biz Bob Sam")))
      printCurrent()
      routes.linkTo(MagicScreen(thing,FakeUser(1001,"That Dude")))
      printCurrent()
      dom.window.history.back()
      dom.setTimeout(() => {
        println("GO BACK!")
        printCurrent()
      },10)
    }

    'test2 {
      val routes = Router.generate[Screen](FooScreen)
      println("~~~~~~~ DO THE OTHER THING! ~~~~~~~~~")
      val thing = OtherRandomThing(10,"MY-RANDOM-THING-NAME")
      routes.linkTo(MagicScreen(thing,FakeUser(1001,"That Dude")))
      routes.linkTo(MagicScreen(thing,FakeUser(1001,"That Dude")))
      routes.linkTo(MagicScreen(thing,FakeUser(1001,"That Dude")))
      println("WAAAT")
      println(routes.current.now)
      println(dom.window.location.href)
      routes.parseUrl("/foo").map(println)
      routes.parseUrl("/bar").map(println)
      routes.parseUrl("/baz").map(println)
      routes.parseUrl("/profile/99").map(println)
      routes.parseUrl("/friend/99").map(println)
      routes.parseUrl("/magic/10/MY-RANDOM-THING-NAME/AND-AlSO-WHO/1001").map(println)
    }

    'prefixTest {
      sealed trait AdminScreen
      case object Foo2Screen extends AdminScreen
      case object Bar2Screen extends AdminScreen
      val routes: Router[AdminScreen] = Router.generate[AdminScreen](Foo2Screen)
      println(dom.window.location.href)
      println(dom.window.location.href)
      println(dom.window.location.href)
    }
  }
}
