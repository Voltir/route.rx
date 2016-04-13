package routerx

import upickle.default._
import org.scalajs.dom
import org.scalajs.dom.raw.PopStateEvent
import rx._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.language.experimental.macros
import internal._

class Router[Link](default: Link, val table: RouteTable[Link], prefix: Option[String] = None)(implicit R: Reader[Link], w: Writer[Link]) {

  private val urlPrefix = prefix.map(p => if(p.startsWith("/")) p else "/" + p).getOrElse("")

  private def updateBrowserHistory(stateFunc: (js.Any,String,String) => Unit)(link: Link) = {
    val fullUrl = urlPrefix + table.urlFor(link)
    if(table.isVolatileLink(link)) {
      stateFunc(null, null, fullUrl)
    } else {
      stateFunc(write(link), null, fullUrl)
    }
  }

  val current: Var[Link] = Var(default)

  def goto(link: Link): Unit = {
    updateBrowserHistory(dom.window.history.pushState)(link)
    current() = link
  }

  def switchTo(link: Link): Unit = {
    updateBrowserHistory(dom.window.history.replaceState)(link)
    current() = link
  }

  def parseUrl(url: String)(implicit ec: ExecutionContext): Future[Link] = {
    table.linkGiven(url.drop(urlPrefix.length),default)
  }

  dom.window.onpopstate = { (evt: PopStateEvent) =>
    if(evt.state != null) {
      val link = read[Link](evt.state.asInstanceOf[String])
      current() = link
    }
    else {
      val urlDynamicPart = dom.window.location.pathname.drop(urlPrefix.length)
      parseUrl(urlDynamicPart).map { link =>
        current() = link
      }
    }
  }
}

object Router {
  import routerx.internal._
  def generate[Link](default: Link)(implicit R: Reader[Link], w: Writer[Link]): Router[Link] =
    macro Macros.generateRouter[Link]

  def generateWithPrefix[Link](default: Link,
                               urlPrefix: String)
                              (implicit R: Reader[Link], w: Writer[Link]): Router[Link] =
    macro Macros.generateRouterWithPrefix[Link]
}