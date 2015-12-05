package locallink.implicits

import likelib.{IntLike, LongLike}
import locallink.UrlPart

import scala.concurrent.{ExecutionContext, Future}

trait LikeTypes {
  //Useful for SQL ids in URLS
  implicit def LongLikePart[T: LongLike]: UrlPart[T] = new UrlPart[T] {
    val size = 1
    def toParts(inp: T) = implicitly[LongLike[T]].to(inp).toString :: Nil
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[T] = {
      Future.successful(implicitly[LongLike[T]].from(inp.head.toLong))
    }
  }

  implicit def IntLikePart[T: IntLike]: UrlPart[T] = new UrlPart[T] {
    val size = 1
    def toParts(inp: T) = implicitly[IntLike[T]].to(inp).toString :: Nil
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[T] = {
      Future.successful(implicitly[IntLike[T]].from(inp.head.toInt))
    }
  }
}
