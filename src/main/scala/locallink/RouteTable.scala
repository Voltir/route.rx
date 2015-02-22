package locallink

import scala.concurrent.{ExecutionContext, Future}

trait RouteTable[Link] {
  def urlFor(link: Link): String
  def isVolatileLink(link: Link): Boolean
  def linkGiven(url: String, onError: Link)(implicit ec: ExecutionContext): Future[Link]
}
