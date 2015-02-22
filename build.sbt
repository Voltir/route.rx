enablePlugins(ScalaJSPlugin)

name := "local-link"

organization := "com.stabletech"

version := "0.0.3-SNAPSHOT"

scalaVersion := "2.11.5"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"
libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.4.5"
libraryDependencies += "com.lihaoyi" %%% "scalarx" % "0.2.7"
libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.2.6"
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value 

jsDependencies += RuntimeDOM

skip in packageJSDependencies := false

// uTest settings
libraryDependencies += "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
testFrameworks += new TestFramework("utest.runner.Framework")

persistLauncher in Compile := true
persistLauncher in Test := false

scalaJSStage in Global := FastOptStage
requiresDOM := true

publishTo := Some(Resolver.file("Github Pages", new File("/home/nick/publish/local-link")))
