organization := "org.scalablytyped"
name := "mongoose-simple-random"
version := "0.4-c7e55d"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "mongoose" % "0.0-unknown-4a5c1e",
  "org.scalablytyped" %%% "node" % "0.0-unknown-30d532",
  "org.scalablytyped" %%% "std" % "0.0-unknown-86ca84")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
