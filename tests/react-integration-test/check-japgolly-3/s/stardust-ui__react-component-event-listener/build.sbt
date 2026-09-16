organization := "org.scalablytyped"
name := "stardust-ui__react-component-event-listener"
version := "0.38.0-8a7b14"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % "4.0.0",
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "react" % "16.9.2-ccab7c",
  "org.scalablytyped" %%% "std" % "0.0-unknown-19748e")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
