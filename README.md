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
"com.stabletechs" %%% "local-link" % "0.0.7"
```

local-link is currently only compiled for Scala.js 0.6+

Quick Demo
==========
[Demo](https://voltir.github.io/local-link-demo)

[Demo Source](https://github.com/Voltir/local-link)
