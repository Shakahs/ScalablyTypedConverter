organization := "org.scalablytyped"
name := "angular-agility"
version := "0.0-unknown-a1c885"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "angular" % "1.6-1e5457",
  "org.scalablytyped" %%% "std" % "0.0-unknown-dee0eb")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
