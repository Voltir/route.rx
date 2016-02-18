package routerx.implicits

import routerx.UrlPart


trait OptionAsUpsert {

  import scala.concurrent.{ExecutionContext,Future}

  implicit def optionAsUpsert[T: UrlPart]: UrlPart[Option[T]] = new UrlPart[Option[T]] {
    val urlpart = implicitly[UrlPart[T]]
    val size = 1 + urlpart.size

    def toParts(inp: Option[T]) = inp.map{ elem =>
      "edit" :: urlpart.toParts(elem)
    }.getOrElse{
      "create" :: List.fill(urlpart.size)("new")
    }

    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[Option[T]] = {
      parts match {
        case "edit" :: tail => urlpart.fromParts(tail)(ec).map(Option.apply)
        case "create" :: tail => Future.successful(None)
        case _ => Future.failed(new Throwable("invalid url"))
      }
    }
  }
}
