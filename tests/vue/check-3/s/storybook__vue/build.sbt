organization := "org.scalablytyped"
name := "storybook__vue"
version := "3.3-b8efe5"
scalaVersion := "3.9.0"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "std" % "0.0-unknown-0feb61",
  "org.scalablytyped" %%% "vue" % "2.5.13-8f205f",
  "org.scalablytyped" %%% "webpack-env" % "1.13-83b7fa")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
