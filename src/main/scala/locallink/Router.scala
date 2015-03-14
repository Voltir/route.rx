package locallink

import upickle._
import org.scalajs.dom
import org.scalajs.dom.raw.PopStateEvent
import rx._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.language.experimental.macros
import internal._

class Router[Link](default: Link, table: RouteTable[Link])(implicit R: upickle.Reader[Link], w: upickle.Writer[Link]) {

  private def updateBrowserHistory(stateFunc: (js.Any,String,String) => Unit)(link: Link) = {
    if(table.isVolatileLink(link)) {
      stateFunc(null, null, table.urlFor(link))
    } else {
      stateFunc(upickle.write(link), null, table.urlFor(link))
    }
  }

  val current: Var[Link] = Var(default)

  def goto(link: Link): Unit = {
    updateBrowserHistory(dom.window.history.pushState)(link)
    current() = link
  }

  def switchTo(link: Link): Unit = {
    updateBrowserHistory(dom.window.history.replaceState)(default)
    current() = link
  }

  def parseUrl(url: String)(implicit ec: ExecutionContext): Future[Link] = {
    table.linkGiven(url,default)
  }

  dom.window.onpopstate = { (evt: PopStateEvent) =>
    if(evt.state != null) {
      val link = upickle.read[Link](evt.state.asInstanceOf[String])
      current() = link
    }
    else {
      parseUrl(dom.window.location.pathname).map { link =>
        current() = link
      }
    }
  }
}

object Router {
  import locallink.internal._
  def generate[Link](default: Link)(implicit R: upickle.Reader[Link], w: upickle.Writer[Link]): Router[Link] = macro Macros.generateRouter[Link]
}