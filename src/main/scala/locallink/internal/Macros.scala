package locallink.internal

import locallink.{VolatileLink, RouteTable, Router}

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

  private def urlRoot(c: blackbox.Context)(urlPrefix: String, baseScreenTraitFullName: String): String = {
    val rootSymbolName = baseScreenTraitFullName.split('.').toList.lastOption.getOrElse("Screen")
    if(!rootSymbolName.endsWith("Screen")) {
      c.abort(c.enclosingPosition,"Route traits and objects must end in 'Screen'")
    }
    val nameSize = rootSymbolName.length - "Screen".length
    s"${urlPrefix}${if(nameSize > 0) rootSymbolName.take(nameSize).toLowerCase+"/" else ""}"
  }

  private def generateLinksToUrl(c: blackbox.Context)(urlPrefix: String, baseScreenTrait: c.universe.ClassSymbol): Set[c.universe.Tree] = {
    import c.universe._

    if(baseScreenTrait.knownDirectSubclasses.isEmpty) {
      c.abort(c.enclosingPosition, "Error with knownDirectSubclasses (SI-7046)")
    }

    val rootPart = urlRoot(c)(urlPrefix,baseScreenTrait.fullName)

    baseScreenTrait.knownDirectSubclasses.map { sym =>
      val isCaseObject = sym.asClass.isCaseClass && sym.asClass.isModuleClass
      val isCaseClass = sym.asClass.isCaseClass && !sym.asClass.isModuleClass
      val isSealedTrait = sym.asClass.isTrait && sym.asClass.isSealed
      if (isCaseObject) urlForCaseObject(c)(rootPart, sym)
      else if (isCaseClass) urlForCaseClass(c)(rootPart, sym)
      else if (isSealedTrait)  {
        cq"e: ${sym.asType} => e match { case ..${generateLinksToUrl(c)(rootPart,sym.asClass)} }"
      }
      else {
        c.abort(c.enclosingPosition, "Unknown Subclass Type!")
      }
    }
  }

  private def generateUrlToLinks[Link: c.WeakTypeTag](c: blackbox.Context)(rootPart: String, baseScreenTrait: c.universe.ClassSymbol): Set[c.universe.Tree] = {
    import c.universe._

    val linkTpe = weakTypeTag[Link].tpe

    baseScreenTrait.knownDirectSubclasses.map { sym =>

      if(!sym.fullName.endsWith("Screen")) {
        c.abort(c.enclosingPosition,"All link types must end in Screen")
      }

      val isCaseObject = sym.asClass.isCaseClass && sym.asClass.isModuleClass
      val isCaseClass = sym.asClass.isCaseClass && !sym.asClass.isModuleClass
      val isSealedTrait = sym.asClass.isTrait && sym.asClass.isSealed

      if(isCaseObject) {
        val url = prefix(rootPart, sym.fullName)
        val basicCaseObject = q"{ case ((_:String),(ec:ExecutionContext)) => Future.successful(${sym.asClass.module})}"
        q"$url -> $basicCaseObject"
      }

      else if(isSealedTrait) {
        val prefix = urlRoot(c)(rootPart,sym.fullName)
        val omg = generateUrlToLinks(c)("/",sym.asClass)
        val subtable = q"""{ case ((remaining:String),(ec:ExecutionContext)) =>
          val unurl: Map[String, (String,ExecutionContext) => Future[$linkTpe]] = Map(..$omg)
          val asurl = "/"+remaining
          unurl
            .toList
            .sortBy(_._1)(implicitly[Ordering[String]].reverse)
            .find(part => asurl.startsWith(part._1))
            .map
          { case (matched,builder) =>
            builder(asurl.drop(matched.size),ec)
          }.get
        }"""
        q"""$prefix  -> $subtable"""
      }

      else if(isCaseClass) {
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
          //""""""
        }
        val accessorsTerms = (0 until accessors.size).map { idx => TermName(s"acc$idx") }
        val builder = q"""{ case ((remaining:String),(ec:ExecutionContext)) =>
          var $unusedParts = remaining.split('/').toList.drop(1)
          for (..$elemPartials) yield { $companion.apply(..$accessorsTerms) }
        }"""
        q"$url -> $builder"
      }

      else {
        c.abort(c.enclosingPosition,"Unknown Subclass Type!")
      }
    }
  }

  private def generateVolatileLinks(c: blackbox.Context)(baseScreenTrait: c.universe.ClassSymbol): Set[c.universe.Tree] = {
    import c.universe._
    val volatileLinkTpe = weakTypeTag[VolatileLink].tpe
    baseScreenTrait.knownDirectSubclasses.flatMap { sym =>
      val isSealedTrait = sym.asClass.isTrait && sym.asClass.isSealed
      if(sym.asClass.baseClasses.exists(_.asType.toType <:< volatileLinkTpe)) {
        List(cq"_: ${sym.asType} => true")
      }
      else if(isSealedTrait) {
        generateVolatileLinks(c)(sym.asClass)
      }
      else List.empty
    }
  }

  def generateRouter[Link: c.WeakTypeTag](c: blackbox.Context)(default: c.Expr[Link])(R: c.Expr[upickle.Reader[Link]], w: c.Expr[upickle.Writer[Link]]): c.Expr[Router[Link]] = {
    import c.universe._
    val linkTpe = weakTypeTag[Link].tpe

    if(!linkTpe.typeSymbol.asClass.isTrait || !linkTpe.typeSymbol.asClass.isSealed) {
      c.abort(c.enclosingPosition, "Routes may only use a sealed trait")
    }
    val clsSymbol = linkTpe.typeSymbol.asClass
    val linkToUrl = generateLinksToUrl(c)("/",clsSymbol)
    val urlToLink = generateUrlToLinks[Link](c)(urlRoot(c)("/",clsSymbol.fullName),clsSymbol)
    val volatileLinks = generateVolatileLinks(c)(clsSymbol)

    val table = c.Expr[RouteTable[Link]](q"""
      new RouteTable[$linkTpe] {
        private val unUrl: Map[String, (String,ExecutionContext) => Future[$linkTpe]] = Map(..$urlToLink)

        def urlFor(link: $linkTpe): String = link match { case ..$linkToUrl }

        def isVolatileLink(link: $linkTpe): Boolean = link match {
          case ..$volatileLinks
          case _ => false
        }

        def linkGiven(url: String, onError: $linkTpe)(implicit ec: ExecutionContext): Future[$linkTpe] = {
          unUrl
            .toList
            .sortBy(_._1)(implicitly[Ordering[String]].reverse)
            .find(part => url.startsWith(part._1)).map
          { case (matched,builder) =>
            val remaining = url.drop(matched.size)
            builder(remaining,ec).recover { case err  => onError }
          }.getOrElse(Future(onError))
        }
      }
    """)

    c.Expr[Router[Link]](q"new Router[$linkTpe]($default,$table)")
  }
}