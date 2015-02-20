import scala.concurrent.{Future, ExecutionContext}

package object locallink {
  import scala.language.experimental.macros

  //trait LocalLink[Target] {
  //  def asUrl(t: Target): String //= macro Macros.magic[Target]
  //}

  trait UrlPartial[T] {
    val numParts: Int
    def toParts(inp: T): List[String]
    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[T]
  }
}
