import scala.concurrent.{Future, ExecutionContext}
import scala.annotation.StaticAnnotation

package object locallink {
  /**
   * UrlPart defines how to take arbitrary screen parameters and represent them as part of the generated URL.
   * The basic idea is that enough information will be stored in the url, such as an object id,
   * that it is possible to reconstruct the object if required.
   *
   * - Size is required and must match the length taken from toParts and given to fromParts
   */
  trait UrlPart[T] {
    val size: Int
    def toParts(inp: T): List[String]
    def fromParts(parts: List[String])(implicit ec: ExecutionContext): Future[T]
  }

  /**
   * This trait is used to skip the state cache used in the browsers History API
   * And will in effect force a reload during a popstate event
   */
  trait VolatileLink

  class fragment(f: String) extends StaticAnnotation
}
