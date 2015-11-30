enablePlugins(ScalaJSPlugin)

name := "local-link"

organization := "com.stabtechs"

version := "0.0.7-SNAPSHOT"

scalaVersion := "2.11.7"

homepage := Some(url("http://stabtechs.com/"))

licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php"))

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.2"
libraryDependencies += "com.lihaoyi" %%% "scalarx" % "0.3.1-SNAPSHOT"
libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.4"
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value 

jsDependencies += RuntimeDOM

skip in packageJSDependencies := false

// uTest settings
libraryDependencies += "com.lihaoyi" %%% "utest" % "0.3.1"
testFrameworks += new TestFramework("utest.runner.Framework")

persistLauncher in Compile := true
persistLauncher in Test := false

scalaJSStage in Global := FastOptStage
requiresDOM := true

//publishTo := Some(Resolver.file("Github Pages", new File("/home/nick/publish/local-link")))

scmInfo := Some(ScmInfo(
    url("https://github.com/Voltir/local-link"),
    "scm:git:git@github.com/Voltir/local-link.git",
    Some("scm:git:git@github.com/Voltir/local-link.git")))

publishMavenStyle := true
publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

sonatypeProfileName := "com.stabtechs"

pomExtra := (
  <developers>
    <developer>
      <id>Voltaire</id>
      <name>Nick Childers</name>
      <url>https://github.com/voltir/</url>
    </developer>
  </developers>
)

pomIncludeRepository := { _ => false }
