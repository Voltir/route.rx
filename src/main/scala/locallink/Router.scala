package locallink


import upickle._
import org.scalajs.dom
import org.scalajs.dom.raw.PopStateEvent
import rx._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import internal._

class Router[Link](default: Link, table: RouteTable[Link])(implicit R: upickle.Reader[Link], w: upickle.Writer[Link]) {
  val current: Var[Link] = {
    val url = table.urlFor(default)
    dom.window.history.pushState(upickle.write(default),null,url)
    Var(default)
  }

  def linkTo(link: Link): Unit = {
    val url = table.urlFor(link)
    dom.window.history.pushState(upickle.write(link),null,url)
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
      println("TODO LINK STATE NULL!")
      //assert(false,"Need to do something to build a Link without a valid state!")
    }
  }
}

object Router {
  import locallink.internal._
  def generate[Link](default: Link)(implicit R: upickle.Reader[Link], w: upickle.Writer[Link]): Router[Link] = macro locallink.internal.Macros.generateRouter[Link]
}