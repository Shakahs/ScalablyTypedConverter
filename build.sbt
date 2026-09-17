import scala.sys.process.stringToProcess

lazy val latestTag =
  "git tag -l --sort=committerdate".!!.linesIterator.toVector.lastOption.fold("no-version")(_.drop( /* 'v' */ 1))

ThisBuild / scalaVersion := Versions.scala3

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,
  "org.scala-lang.modules" %% "scala-collection-compat" % VersionScheme.Always,
)

lazy val core = project
  .in(file("core"))
  .configure(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      Deps.osLib.cross(CrossVersion.for3Use2_13),
      Deps.sourcecode.exclude("org.scala-lang.modules", "scala-collection-compat_3"),
      Deps.ammoniteOps.cross(CrossVersion.for3Use2_13),
    ) ++ Deps.circe.map(_.exclude("org.scala-lang.modules", "scala-collection-compat_3")),
  )

lazy val logging = project
  .in(file("logging"))
  .configure(baseSettings)
  .settings(libraryDependencies ++= Seq(Deps.sourcecode, Deps.fansi))

lazy val ts = project
  .in(file("ts"))
  .configure(baseSettings)
  .dependsOn(core, logging)
  .settings(libraryDependencies += Deps.parserCombinators)

lazy val docs = project
  .in(file("converter-docs"))
  .settings(
    mdocVariables := Map("VERSION" -> latestTag),
    moduleName := "converter-docs",
    publish / skip := true,
  )
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

lazy val scalajs = project
  .in(file("scalajs"))
  .dependsOn(core, logging)
  .configure(baseSettings)
  .settings(libraryDependencies ++= Seq(Deps.scalaXml))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "org.scalablytyped.converter.internal.scalajs",
    buildInfoKeys := Seq[BuildInfoKey](
      "scala213" -> Versions.scala213,
      "scala3" -> Versions.scala3,
      "scalaJs" -> Versions.scalaJs,
    ),
  )

lazy val phases = project
  .in(file("phases"))
  .dependsOn(core, logging)
  .configure(baseSettings)
  .settings(libraryDependencies ++= Seq(Deps.ox, Deps.scalatest % Test))

lazy val `importer-portable` = project
  .in(file("importer-portable"))
  .configure(baseSettings)
  .dependsOn(ts, scalajs, phases)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "org.scalablytyped.converter.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      "gitSha" -> "git rev-parse -1 HEAD".!!.split("\n").last.trim,
      "version" -> version.value,
    ),
  )

lazy val importer = project
  .in(file("importer"))
  .dependsOn(`importer-portable`)
  .configure(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      Deps.coursier.cross(CrossVersion.for3Use2_13).exclude("org.scala-lang.modules", "scala-xml_2.13"),
      Deps.scalaXml,
      Deps.scalatest % Test,
    ),
    Test / fork := true,
    assembly / test := {},
    assembly / mainClass := Some("org.scalablytyped.converter.Main"),
    /* meh meh meh */
    assembly / assemblyMergeStrategy := {
      case foo if foo.contains("io/github/soc/directories/")         => MergeStrategy.first
      case foo if foo.contains("reflect.properties")                 => MergeStrategy.first
      case foo if foo.contains("scala-collection-compat.properties") => MergeStrategy.first
      case foo if foo.endsWith("module-info.class")                  => MergeStrategy.discard
      case foo if foo.contains("org/fusesource")                     => MergeStrategy.first
      case foo if foo.contains("META-INF/native/")                   => MergeStrategy.first
      case foo if foo.contains("scala/annotation")                   => MergeStrategy.last
      case foo if foo.contains("META-INF/sisu/javax.inject.Named")   => MergeStrategy.discard
      case other                                                     => (assembly / assemblyMergeStrategy).value(other)
    },
    Test / testOptions += Tests.Argument("-P4"),
  )

lazy val cli = project
  .in(file("cli"))
  .dependsOn(importer)
  .configure(baseSettings)
  .settings(
    libraryDependencies ++= Seq(Deps.scopt, Deps.scalatest % Test),
  )

lazy val `import-scalajs-definitions` = project
  .in(file("import-scalajs-definitions"))
  .configure(baseSettings)
  .dependsOn(importer)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scalap" % Versions.scala213,
      Deps.coursier.cross(CrossVersion.for3Use2_13).exclude("org.scala-lang.modules", "scala-xml_2.13"),
    ),
    publish / skip := true,
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "converter-root",
    publish / skip := true,
  )
  .aggregate(logging, core, phases, ts, scalajs, `importer-portable`, importer, cli)

lazy val baseSettings: Project => Project =
  _.settings(
    organization := "io.kinoplan.scalablytyped",
    licenses += ("GPL-3.0", url("https://opensource.org/licenses/GPL-3.0")),
    homepage := Some(url("https://github.com/kinoplan/Converter")),
    developers := List(
      Developer(
        "oyvindberg",
        "Øyvind Raddum Berg",
        "elacin@gmail.com",
        url("https://github.com/oyvindberg"),
      ),
    ),
    scalacOptions ~= (_.filterNot(Set("-Ywarn-unused:imports", "-Ywarn-unused:params", "-Xfatal-warnings"))),
    scalacOptions ++= Seq("-no-indent", "-source:3.3"),
    /* scalatest assertions return `Assertion`, which is discarded in the middle of a test */
    Test / scalacOptions -= "-Wnonunit-statement",
    /* disable scaladoc */
    Compile / doc / sources := Nil,
  )
