addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.15")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.1")

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
