package locallink

import scala.concurrent.{ExecutionContext, Future}

trait RouteTable[Link] {
  def urlFor(link: Link): String
  def linkGiven(url: String, onError: Link)(implicit ec: ExecutionContext): Future[Link]
}
