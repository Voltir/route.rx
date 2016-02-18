package routerx.implicits

import routerx.UrlPart

trait PrimitiveTypes {
  import scala.concurrent.{ExecutionContext,Future}

  implicit val StringPart = new UrlPart[String] {
    val size = 1
    def toParts(inp: String) = List(inp)
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[String] = Future.successful(inp.head)
  }

  implicit val LongPart: UrlPart[Long] = new UrlPart[Long] {
    val size = 1
    def toParts(inp: Long) = List(inp.toString)
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[Long] = Future.successful(inp.head.toLong)
  }

  implicit val IntPart: UrlPart[Int] = new UrlPart[Int] {
    val size = 1
    def toParts(inp: Int) = List(inp.toString)
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[Int] = Future.successful(inp.head.toInt)
  }

  implicit val FloatPart: UrlPart[Float] = new UrlPart[Float] {
    val size = 1
    def toParts(inp: Float) = List(inp.toString)
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[Float] = Future.successful(inp.head.toFloat)
  }

  implicit val DoublePart: UrlPart[Double] = new UrlPart[Double] {
    val size = 1
    def toParts(inp: Double) = List(inp.toString)
    def fromParts(inp: List[String])(implicit ec: ExecutionContext): Future[Double] = Future.successful(inp.head.toDouble)
  }

  implicit val BooleanPart: UrlPart[Boolean] = new UrlPart[Boolean] {
    val size = 1
    def toParts(inp: Boolean) = if(inp) "t" :: Nil else "f" :: Nil
    def fromParts(inp: List[String])(implicit ec: ExecutionContext) = inp match {
      case "t" :: Nil => Future.successful(true)
      case "f" :: Nil => Future.successful(false)
      case _ => Future.failed(new Throwable("Invalid Boolean Argument!"))
    }
  }
}
