organization := "org.scalablytyped"
name := "numjs"
version := "0.0-unknown-0ea755"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "ndarray" % "0.0-unknown-285a03",
  "org.scalablytyped" %%% "std" % "0.0-unknown-d5338e")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
