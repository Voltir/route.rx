package locallink.internal

import locallink.{RouteTable, Router}

import scala.language.experimental.macros
import scala.reflect.macros._

object Macros {

  def getCompanion(c: blackbox.Context)(tpe: c.Type) = {
    import c.universe._
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
    c.universe.internal.gen.mkAttributedRef(pre, tpe.typeSymbol.companion)
  }

  def prefix(rootPart: String, fullName: String): String = {
    val name = fullName.toLowerCase
    val hackIdx = name.lastIndexOf('.')
    val whatever = name.drop(hackIdx+1)
    rootPart+whatever.take(whatever.length - "Screen".size).toLowerCase
  }

  private def urlForCaseObject(c: blackbox.Context)(rootPart: String, sym: c.universe.Symbol): c.Tree = {
    import c.universe._
    cq"e: ${sym.asType} => ${prefix(rootPart, sym.fullName)}"
  }

  private def urlForCaseClass(c: blackbox.Context)(rootPart: String, sym: c.universe.Symbol): c.Tree = {
    import c.universe._
    val term = TermName("e")
    val toParts = sym.asType.info.decls.filter(_.asTerm.isAccessor).map { acc =>
      q"implicitly[UrlPart[${acc.asTerm}]].toParts($term.$acc)"
    }
    cq"""$term: ${sym.asType} => {
      val allParts = List(..$toParts).flatten
      ${prefix(rootPart, sym.fullName)} + "/" + allParts.mkString("/")
    }"""

  }

  def generateRouter[Link: c.WeakTypeTag](c: blackbox.Context)(default: c.Expr[Link])(R: c.Expr[upickle.Reader[Link]], w: c.Expr[upickle.Writer[Link]]): c.Expr[Router[Link]] = {
    import c.universe._

    val linkTpe = weakTypeTag[Link].tpe

    if(!linkTpe.typeSymbol.asClass.isTrait || !linkTpe.typeSymbol.asClass.isSealed) {
      c.abort(c.enclosingPosition, "Routes may only use a sealed trait")
    }

    println(linkTpe.typeSymbol.asClass.fullName.split('.').toList)
    val rootSymbolName = linkTpe.typeSymbol.asClass.fullName.split('.').toList.lastOption.getOrElse("Screen")
    if(!rootSymbolName.endsWith("Screen")) {
      c.abort(c.enclosingPosition,"Base route must end in 'Screen'")
    }

    val rootPart =
      if(rootSymbolName == "Screen") "/"
      else "/" + rootSymbolName.take(rootSymbolName.length - "Screen".length).toLowerCase + "/"

    val clsSymbol = linkTpe.typeSymbol.asClass

    if(clsSymbol.knownDirectSubclasses.isEmpty) {
      c.abort(c.enclosingPosition, "Error with knownDirectSubclasses (SI-7046)")
    }

    val linkToUrl = clsSymbol.knownDirectSubclasses.map { sym =>
      val isCaseObject = sym.asClass.isCaseClass && sym.asClass.isModuleClass
      val isCaseClass = sym.asClass.isCaseClass && !sym.asClass.isModuleClass
      if (isCaseObject) urlForCaseObject(c)(rootPart,sym)
      else if (isCaseClass) urlForCaseClass(c)(rootPart,sym)
      else {
        c.abort(c.enclosingPosition,"Unknown Subclass Type!")
      }
    }

    val urlToLink = clsSymbol.knownDirectSubclasses.map { sym =>

      if(!sym.fullName.endsWith("Screen")) {
        c.abort(c.enclosingPosition,"All link types must end in Screen")
      }

      val isCaseObject = sym.asClass.isCaseClass && sym.asClass.isModuleClass

      val isCaseClass = sym.asClass.isCaseClass && !sym.asClass.isModuleClass

      if(isCaseObject) {
        val url = prefix(rootPart, sym.fullName)
        val basicCaseObject = q"{ case ((_:String),(ec:ExecutionContext)) => Future(${sym.asClass.module})(ec)}"
        q"$url -> $basicCaseObject"
      } else {
        val url = prefix(rootPart, sym.fullName)

        val companion = getCompanion(c)(sym.asType.toType)

        val unusedParts = TermName("unusedParts")

        val accessors = sym.asType.info.decls.filter(_.asTerm.isAccessor)

        val elemPartials = accessors.zipWithIndex.map { case (acc,idx) =>
          val accTerm = TermName(s"acc$idx")
          val thing = q"implicitly[UrlPart[${acc.asTerm}]]"
          fq"""$accTerm <- {
              val partial = $thing
              val r = partial.fromParts($unusedParts.take(partial.size))
              $unusedParts = $unusedParts.drop(partial.size)
              r
          }"""
        }

        val accessorsTerms = (0 until accessors.size).map { idx => TermName(s"acc$idx") }

        val builder = q"""{ case ((remaining:String),(ec:ExecutionContext)) =>
          var $unusedParts = remaining.split('/').toList.drop(1)
          for (..$elemPartials) yield { $companion.apply(..$accessorsTerms) }
        }"""

        q"$url -> $builder"
      }
    }

    val table = c.Expr[RouteTable[Link]](q"""
      new RouteTable[$linkTpe] {
        def urlFor(link: $linkTpe): String = link match { case ..$linkToUrl }

        private val unUrl: Map[String, (String,ExecutionContext) => Future[$linkTpe]] = Map(..$urlToLink)

        def linkGiven(url: String, onError: $linkTpe)(implicit ec: ExecutionContext): Future[$linkTpe] = {
          unUrl.find(part => url.startsWith(part._1)).map { case (matched,builder) =>
            val remaining = url.drop(matched.size)
            builder(remaining,ec).recover { case err =>
              println("Error building link: " + err.getMessage)
              onError
            }
          }.getOrElse(Future(onError))
        }
      }
    """)

    c.Expr[Router[Link]](q"new Router[$linkTpe]($default,$table)")
  }

}