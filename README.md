Local Link 0.0.7
================
Macro magic to make "reactive" urls.

```scala    
//Define the set of "Screens" the app has
sealed trait Screen
case object IndexScreen extends Screen
case object AboutScreen extends Screen
case object UsersScreen extends Screen
case class ProfileScreen(user: User) extends Screen

object Demo extends js.JSApp {

  //Macro Magic
  val router = Router.generate[Screen](IndexScreen)

  //and router is now bound to the browser history api :)

  private lazy val current: Rx[HtmlTag] = Rx {
    router.current() match {
      case IndexScreen => screens.Index.screen()
      case AboutScreen => screens.About.screen()
      case UsersScreen => screens.Users.screen()
      case ProfileScreen(user) => screens.Profile.screen(user)
    }
  }

  lazy val view = {
    div(current)
  }

  def main(): Unit = {
    dom.document.body.appendChild(view.render)
  }
}
```

Getting Started
===============


```scala
"com.stabletechs" %%% "local-link" % "1.0.0"
```

local-link is currently only compiled for Scala.js 0.6+

Quick Demo
==========
[Demo](https://voltir.github.io/local-link-demo)

[Demo Source](https://github.com/Voltir/local-link)

Details and Limitations
=======================

##Browser Only
This library manipulates the browser history object through the JavaScript API. In order to make the app behave as if each screen really was its own "webpage", the server must route the urls properly to the app, and the app needs to read the url basically at page load, for example: 
```scala
@JSExport
  def main(): Unit = {
    router.parseUrl(dom.window.location.pathname).flatMap { route =>
      router.goto(route)
    }
  }
}
```

##Base trait needs to end in 'Screen'
Currently, it is required that the base sealed trait end with the word "Screen". The URL string is automatically generated from that trait and its children, and the suffix "Screen" is currently hard-coded into that logic. For example:
```scala
sealed trait AdminScreen
case class UserScreen(u: User) extends AdminScreen
case object DashboardScreen extends AdminScreen
```
will generate "/admin/user" and "/admin/dashboard".

The logic doesn't have to be that way, so if there is interest in doing it some other way, that could be explored.

##Fragment Annotation
It is possible to override part of the url generation with the @fragment annotation, for example
```scala
sealed trait AdminScreen

trait UserScreen extends AdminScreen
case object UserProfileScreen extends UserScreen

trait AccountScreen extends AdminScreen
case object AccountProfileScreen extends AccountScreen
```
would normally generate "/admin/user/user-profile" and "/admin/account/account-profile" as the auto generated URL, however

```scala
sealed trait AdminScreen

trait UserScreen extends AdminScreen
@fragment("profile") case object UserProfileScreen extends UserScreen

trait AccountScreen extends AdminScreen
@fragment("profile") case object AccountProfileScreen extends AccountScreen
```
will generate "/admin/user/profile" and "/admin/account/profile"  instead. 

##UrlPart Typeclass
In the examples, there are instances of case classes (eg User) that occur in the Screen trait hierarchy - while these can be arbitrary case classes, this works through the use of a "UrlPart" Typeclass, which defines how to map an arbitrary case class to/from the URL representation. 

The typeclass itself is defined as 
```scala
  trait UrlPart[T] {
    val size: Int
    def toParts(inp: T): List[String]
    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[T]
  }
```

Often, these case classes will have something like a UserId, in the case of a User object and the mapping might look something like this (using autowire as an example of retrieving a User object from the server):
```scala
  implicit val UserUrlPart = new UrlPart[User] {
    override val size = 1
    override def toParts(inp: User): List[String] = List(inp.userId.toString)
    override def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[User] = parts match {
      case id :: Nil => API[Public].users.fetchUser(UserId(id.toLong).call()
    }
  }
```

With this implicit in scope, any "Screen" trait can take as a parameter a User object and have it mapped appropriately. Note that normally LocalLink uses upickle to cache the screen object directly in the Browser using the history API, which means that fromParts is pretty much only called in the event of a page load as described above. This means the for rapid prototyping it is often very convenient to skip fromParts:
```scala
  implicit val UserUrlPart = new UrlPart[User] {
    override val size = 1
    override def toParts(inp: User): List[String] = List(inp.userId.toString)
    override def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[User] = ???
  }
```
Just note that doing so will cause a run time error if the user tries to navigate directly (ie copy/paste) to a URL that depends on that typeclass. 
