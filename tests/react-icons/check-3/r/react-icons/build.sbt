organization := "org.scalablytyped"
name := "react-icons"
version := "2.2-fb5e34"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "react" % "0.0-unknown-3cce88",
  "org.scalablytyped" %%% "react-icon-base" % "2.1-663124",
  "org.scalablytyped" %%% "std" % "0.0-unknown-5452c8")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
