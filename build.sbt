enablePlugins(ScalaJSPlugin)

name := "routerx"

organization := "com.stabletechs"

version := "1.1.3-SNAPSHOT"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")

homepage := Some(url("http://stabletechs.com/"))

licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php"))

libraryDependencies += "com.stabletechs" %%% "likelib" % "0.1.3-SNAPSHOT"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
libraryDependencies += "com.lihaoyi" %%% "scalarx" % "0.3.2"
libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.4.4"
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value 

jsDependencies += RuntimeDOM

skip in packageJSDependencies := false

// uTest settings
libraryDependencies += "com.lihaoyi" %%% "utest" % "0.4.5"
testFrameworks += new TestFramework("utest.runner.Framework")

persistLauncher in Compile := true
persistLauncher in Test := false

scalaJSUseRhino in Global := false
requiresDOM := true

scmInfo := Some(ScmInfo(
    url("https://github.com/Voltir/route.rx"),
    "scm:git:git@github.com/Voltir/route.rx.git",
    Some("scm:git:git@github.com/Voltir/route.rx.git")))

publishMavenStyle := true
publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

sonatypeProfileName := "com.stabletechs"

pomExtra :=
  <developers>
    <developer>
      <id>Voltaire</id>
      <name>Nick Childers</name>
      <url>https://github.com/voltir/</url>
    </developer>
  </developers>

pomIncludeRepository := { _ => false }
