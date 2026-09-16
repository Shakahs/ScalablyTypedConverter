organization := "org.scalablytyped"
name := "react-transition-group"
version := "2.0-46ea5b"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "me.shadaj" %%% "slinky-web" % "0.7.5",
  "org.scalablytyped" %%% "react" % "0.0-unknown-38a5d9",
  "org.scalablytyped" %%% "std" % "0.0-unknown-0165af")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
