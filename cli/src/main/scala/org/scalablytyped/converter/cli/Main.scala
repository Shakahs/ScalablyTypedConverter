package org.scalablytyped.converter.cli

import com.olvind.logging.{stdout, storing, LogLevel, Logger}
import fansi.{Attr, Color, Str}
import org.scalablytyped.converter.internal.importer._
import org.scalablytyped.converter.internal.importer.build.{PublishedSbtProject, SbtProject, ScalaCliCompiler}
import org.scalablytyped.converter.internal.importer.documentation.Npmjs
import org.scalablytyped.converter.internal.phases.{PhaseRes, PhaseRunner, RecPhase}
import org.scalablytyped.converter.internal.scalajs.{Name, Versions}
import org.scalablytyped.converter.internal.sets.SetOps
import org.scalablytyped.converter.internal.ts.CalculateLibraryVersion.PackageJsonOnly
import org.scalablytyped.converter.internal.ts.{PackageJson, TsIdentLibrary}
import org.scalablytyped.converter.internal.{constants, files, sets, BuildInfo, InFolder, Json}
import org.scalablytyped.converter.{Flavour, Selection}
import scopt.{OParser, OParserBuilder, Read}

import scala.collection.immutable.SortedSet
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object Main {
  class Paths(base: os.Path) {
    lazy val out: os.Path =
      files.existing(base / "out")
    val node_modules: Option[os.Path] =
      Option(base / "node_modules").filter(files.exists)
    val packageJson: Option[os.Path] =
      Option(base / "package.json").filter(files.exists)
  }

  val DefaultOptions = ConversionOptions(
    useScalaJsDomTypes       = true,
    outputPackage            = Name.typings,
    enableScalaJsDefined     = Selection.All,
    flavour                  = Flavour.Normal,
    ignored                  = SortedSet("typescript"),
    stdLibs                  = SortedSet("es6"),
    expandTypeMappings       = EnabledTypeMappingExpansion.DefaultSelection,
    versions                 = Versions(Versions.Scala3, Versions.ScalaJs1),
    organization             = "org.scalablytyped",
    enableReactTreeShaking   = Selection.None,
    enableLongApplyMethod    = false,
    privateWithin            = None,
    useDeprecatedModuleNames = false,
  )

  case class Config(
      conversion:          ConversionOptions,
      wantedLibs:          SortedSet[TsIdentLibrary],
      inDirectory:         os.Path,
      includeDev:          Boolean,
      includeProject:      Boolean,
      parallelism:         Int,
      outputDirectory:     Option[os.Path],
      parseCacheDirectory: Option[os.Path],
  ) {
    lazy val paths = new Paths(inDirectory)
    def mapConversion(f: ConversionOptions => ConversionOptions) = copy(conversion = f(conversion))
  }

  val DefaultConfig = Config(
    DefaultOptions,
    wantedLibs          = SortedSet(),
    inDirectory         = os.pwd,
    includeDev          = false,
    includeProject      = false,
    parallelism         = math.max(2, Runtime.getRuntime.availableProcessors),
    outputDirectory     = None,
    parseCacheDirectory = None,
  )

  /**
    * Where parsed TypeScript is cached between runs, if anywhere.
    *
    * This used to be a `val` that created a directory under the user's home the moment the
    * class loaded. A run that writes sources for a build system to compile has no business
    * touching anything outside the directories it was given, and a cache that a caller
    * cannot point somewhere is one it cannot isolate — so `--outputDirectory` turns the
    * cache off unless `--parseCacheDirectory` says where to put it.
    */
  def parseCachePath(c: Config): Option[java.nio.file.Path] =
    c.parseCacheDirectory match {
      case Some(directory)                     => Some(files.existing(directory).toNIO)
      case None if c.outputDirectory.isDefined => None
      case None                                => Some(files.existing(constants.defaultCacheFolder / "parse").toNIO)
    }

  val t0 = System.currentTimeMillis

  val logger: Logger[(Array[Logger.Stored], Unit)] =
    storing().zipWith(stdout.filter(LogLevel.warn))

  implicit val ReadsFlavour: Read[Flavour] =
    Read.reads(s => Flavour.byName.getOrElse(s, sys.error(s"'$s' is not among ${Flavour.byName.keys}")))

  implicit val ReadsVersionsScalaJs: Read[Versions.ScalaJs] =
    Read.stringRead.map(Versions.ScalaJs.apply)

  implicit val ReadsVersionsScala: Read[Versions.Scala] =
    Read.stringRead.map(Versions.Scala.apply)

  implicit val ReadsTsIdentLibrary: Read[TsIdentLibrary] =
    Read.stringRead.map(TsIdentLibrary.apply)

  implicit val ReadsProjectName: Read[ProjectName] =
    Read.stringRead.map(ProjectName)

  implicit val ReadsOsPath: Read[os.Path] =
    Read.stringRead.map { str =>
      Try(os.Path(str)) match {
        case Failure(_) =>
          os.pwd / os.RelPath(str)
        case Success(absolute) => absolute
      }
    }

  implicit def ReadsSelection[T: Ordering](implicit ts: Read[Seq[T]]): Read[Selection[T]] = Read.reads {
    case "all"                  => Selection.All
    case "none"                 => Selection.All
    case s if s.startsWith("+") => Selection.NoneExcept(ts.reads(s.drop(1)): _*)
    case s if s.startsWith("-") => Selection.AllExcept(ts.reads(s.drop(1)): _*)
    case _ =>
      sys.error(
        "Syntax: `all`/`none` for Selection.All/Selection.None, `+a,b` for Selection.NoneExcept(a, b), `-a,b` for Selection.AllExcept(a, b)",
      )
  }

  val ParseConversionOptions: OParser[Unit, Config] = {
    val builder: OParserBuilder[Config] = OParser.builder[Config]
    import builder._
    OParser.sequence(
      programName("stc"),
      head(s"ScalablyTyped Converter (version ${BuildInfo.version})"),
      help('h', "help"),
      version('v', "version"),
      opt[os.Path]('d', "directory")
        .action((x, c) => c.copy(inDirectory = x))
        .text("Specify another directory instead of the current directory where your package.json and node_modules is"),
      opt[Boolean]("includeDev")
        .action((x, c) => c.copy(includeDev = x))
        .text("Include dev dependencies"),
      opt[Boolean]("includeProject")
        .action((x, c) => c.copy(includeProject = x))
        .text("Include project in current directory"),
      opt[os.Path]('o', "outputDirectory")
        .action((x, c) => c.copy(outputDirectory = Some(x)))
        .text(
          "Write the generated Scala sources to this directory, one subdirectory per library, and stop there. " +
            "Without it the converter compiles the sources itself and publishes jars to the local ivy repository, " +
            "which needs a Scala compiler resolved over the network",
        ),
      opt[os.Path]("parseCacheDirectory")
        .action((x, c) => c.copy(parseCacheDirectory = Some(x)))
        .text(
          s"Cache parsed typescript here. Defaults to ${constants.defaultCacheFolder / "parse"}, or to no cache at " +
            "all when --outputDirectory is given",
        ),
      opt[Int]("parallelism")
        .validate(x => if (x >= 1) success else failure("--parallelism must be at least 1"))
        .action((x, c) => c.copy(parallelism = x))
        .text(s"How many libraries to convert at the same time (default ${DefaultConfig.parallelism})"),
      opt[Boolean]("useScalaJsDomTypes")
        .action((x, c) => c.mapConversion(_.copy(useScalaJsDomTypes = x)))
        .text(
          "When true (which is the default) uses scala-js-dom types when possible instead of types we translate from typescript in std",
        ),
      opt[Flavour]('f', "flavour")
        .action((x, c) => c.mapConversion(_.copy(flavour = x)))
        .text(
          s"One of ${Flavour.byName.keys.mkString(", ")}. See https://scalablytyped.org/docs/flavour",
        ),
      opt[Versions.ScalaJs]("scalajs")
        .action((x, c) => c.mapConversion(cc => cc.copy(versions = cc.versions.copy(scalaJs = x))))
        .text(s"Scala.js version"),
      opt[Versions.Scala]("scala")
        .action((x, c) => c.mapConversion(cc => cc.copy(versions = cc.versions.copy(scala = x))))
        .text(s"Scala version"),
      opt[String]("outputPackage")
        .action((x, c) => c.mapConversion(_.copy(outputPackage = Name(x))))
        .text(s"Output package"),
      opt[Selection[TsIdentLibrary]]("enableScalaJSDefined")
        .action((x, c) => c.mapConversion(_.copy(enableScalaJsDefined = x)))
        .text(s"Libraries you want to enable @ScalaJSDefined traits for."),
      opt[Seq[String]]('s', "stdlib")
        .action((x, c) => c.mapConversion(_.copy(stdLibs = x.toSet.sorted)))
        .text(s"Which parts of typescript stdlib you want to enable"),
      opt[String]("organization")
        .action((x, c) => c.mapConversion(_.copy(organization = x)))
        .text(s"Organization used (locally) publish artifacts"),
      opt[Seq[String]]("ignoredLibs")
        .action((x, c) => c.mapConversion(_.copy(ignored = x.toSet.sorted)))
        .text(s"Libraries you want to ignore"),
      opt[String]("privateWithin")
        .action((x, c) => c.mapConversion(_.copy(privateWithin = Some(Name(x)))))
        .text(s"Libraries you want to ignore"),
      opt[Boolean]("experimentalEnableImplicitOps")
        .action { (x, c) =>
          logger.warn("--experimentalEnableImplicitOps has no effect since it became the default encoding")
          c
        }
        .text(s"Deprecated, doesn't do anything anymore"),
      opt[Boolean]("enableLongApplyMethod")
        .action((x, c) => c.mapConversion(_.copy(enableLongApplyMethod = x)))
        .text(s"Enables long apply methods, instead of implicit ops builders"),
      opt[Boolean]("shortModuleNames")
        .action((x, c) => c.mapConversion(_.copy(useDeprecatedModuleNames = x)))
        .text(
          s"Enables short module names. This used to be the default, and is now deprecated since it's so difficult to navigate",
        ),
      arg[Seq[TsIdentLibrary]]("libs")
        .text("Libraries you want to convert from node_modules")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(wantedLibs = c.wantedLibs ++ x)),
    )
  }

  def table(Key: Attr)(kvs: (String, String)*): Str = {
    val headerLength = kvs.map { case (header, _) => header }.maxBy(_.length).length + 1
    val massaged = kvs.flatMap {
      case (header, value) => Seq[Str](Key(header.padTo(headerLength, ' ')), value, "\n")
    }
    Str.join(massaged)
  }

  val BuildPhase = "build"

  def main(args: Array[String]): Unit = System.exit(mainNoExit(args))

  def mainNoExit(args: Array[String]): Int =
    OParser.parse(ParseConversionOptions, args, DefaultConfig) match {
      case Some(c) =>
        val conversion      = c.conversion
        val libsFromCmdLine = c.wantedLibs
        val inDir           = c.inDirectory
        val includeDev      = c.includeDev
        val includeProject  = c.includeProject
        val parallelism     = c.parallelism

        val packageJsonPath = c.paths.packageJson.getOrElse(sys.error(s"$inDir does not contain package.json"))
        val nodeModulesPath = c.paths.node_modules.getOrElse(sys.error(s"$inDir does not contain node_modules"))
        require(files.exists(nodeModulesPath / "typescript" / "lib"), "must install typescript")

        val packageJson = Json.force[PackageJson](packageJsonPath)

        val projectSource: Option[LibTsSource.FromFolder] =
          if (includeProject) Some(LibTsSource.FromFolder(InFolder(inDir), TsIdentLibrary(inDir.last))) else None

        val wantedLibs: SortedSet[TsIdentLibrary] =
          libsFromCmdLine match {
            case sets.EmptySet() =>
              val fromPackageJson = packageJson.allLibs(includeDev, peer = true).keySet
              require(fromPackageJson.nonEmpty, "No libraries found in package.json")
              val ret = fromPackageJson -- conversion.ignoredLibs
              require(ret.nonEmpty, s"All libraries in package.json ignored")
              ret
            case otherwise => otherwise
          }

        val bootstrapped = Bootstrap.fromNodeModules(InFolder(nodeModulesPath), conversion, wantedLibs)

        val sources: Vector[LibTsSource] = {
          bootstrapped.initialLibs match {
            case Left(unresolved) => sys.error(unresolved.msg)
            case Right(initial)   => projectSource.foldLeft(initial)(_ :+ _)
          }
        }

        println(
          table(fansi.Color.LightBlue)(
            "directory" -> inDir.toString,
            "includeDev" -> includeDev.toString,
            "includeProject" -> includeProject.toString,
            "wantedLibs" -> sources.map(s => s.libName.value).mkString(", "),
            "useScalaJsDomTypes" -> conversion.useScalaJsDomTypes.toString,
            "flavour" -> conversion.flavour.toString,
            "outputPackage" -> conversion.outputPackage.unescaped,
            "enableScalaJsDefined" -> conversion.enableScalaJsDefined.map(_.value).toString,
            "stdLibs" -> conversion.stdLibs.toString,
            "expandTypeMappings" -> conversion.expandTypeMappings.map(_.value).toString,
            "ignoredLibs" -> conversion.ignoredLibs.map(_.value).toString,
//            "ignoredModulePrefixes" -> conversion.ignoredModulePrefixes.toString,
            "versions" -> conversion.versions.toString,
            "organization" -> conversion.organization,
            "enableLongApplyMethod" -> conversion.enableLongApplyMethod.toString,
            "parallelism" -> parallelism.toString,
            "maxHeap" -> s"${Runtime.getRuntime.maxMemory / (1024 * 1024)} MB",
            "processors" -> Runtime.getRuntime.availableProcessors.toString,
          ),
        )

        val ToScalaJs: RecPhase[LibTsSource, LibScalaJs] =
          RecPhase[LibTsSource]
            .next(
              new Phase1ReadTypescript(
                calculateLibraryVersion = PackageJsonOnly,
                resolve                 = bootstrapped.libraryResolver,
                ignored                 = conversion.ignoredLibs,
                ignoredModulePrefixes   = conversion.ignoredModulePrefixes,
                pedantic                = false,
                parser                  = PersistingParser(parseCachePath(c), bootstrapped.inputFolders, logger.void),
                parseParallelism        = parallelism,
                expandTypeMappings      = conversion.expandTypeMappings,
              ),
              "typescript",
            )
            .next(
              new Phase2ToScalaJs(
                pedantic                 = false,
                scalaVersion             = conversion.versions.scala,
                enableScalaJsDefined     = conversion.enableScalaJsDefined,
                outputPkg                = conversion.outputPackage,
                flavour                  = conversion.flavourImpl,
                useDeprecatedModuleNames = conversion.useDeprecatedModuleNames,
              ),
              "scala.js",
            )
            .next(
              new PhaseFlavour(conversion.flavourImpl, maybePrivateWithin = conversion.privateWithin),
              conversion.flavourImpl.toString,
            )

        def run[T](pipeline: RecPhase[LibTsSource, T]): Map[LibTsSource, PhaseRes[LibTsSource, T]] =
          Progress.showing(trackedPhase = BuildPhase, out = System.out) { progress =>
            PhaseRunner.all(pipeline, (_: LibTsSource) => logger.void, progress, parallelism)(sources).toMap
          }

        def reportFailures(failures: Map[LibTsSource, Either[Throwable, String]]): Int = {
          println(
            Color.Red(s"Failure: You might try --ignoredLibs ${failures.keys.map(_.libName.value).mkString(", ")}"),
          )

          failures.foreach {
            case (source, Left(value)) =>
              println(s"${source.libName.value}: (${source.path})")
              value.printStackTrace()
            case (source, Right(value)) =>
              println(s"${source.libName.value}: $value (${source.path})")
          }

          1
        }

        def failuresOf[T](results: Map[LibTsSource, PhaseRes[LibTsSource, T]]) =
          results.collect { case (_, PhaseRes.Failure(errors)) => errors }.reduceOption(_ ++ _).getOrElse(Map.empty)

        val exitCode = c.outputDirectory match {
          /* Sources only: the caller compiles them, so there is no compiler to resolve and
           * nothing is written outside the directory it named. */
          case Some(outputDirectory) =>
            val results = run(
              ToScalaJs.next(
                new Phase3Sources(
                  versions     = conversion.versions,
                  targetFolder = outputDirectory,
                  flavour      = conversion.flavourImpl,
                ),
                BuildPhase,
              ),
            )

            val td = System.currentTimeMillis - t0
            logger.warn(td)

            val failures = failuresOf(results)
            if (failures.nonEmpty) reportFailures(failures)
            else {
              val written: Map[LibTsSource, GeneratedSources] = {
                def go(source: LibTsSource, g: GeneratedSources): Map[LibTsSource, GeneratedSources] =
                  Map(source -> g) ++ g.deps.flatMap { case (k, v) => go(k, v) }

                results.collect { case (s, PhaseRes.Ok(res)) => go(s, res) }.reduceOption(_ ++ _).getOrElse(Map.empty)
              }

              println()
              println(
                s"Wrote sources for ${written.keys.map(x => Color.Green(x.libName.value)).mkString(", ")} " +
                  s"to $outputDirectory",
              )
              0
            }

          case None =>
            val compiler = Await.result(
              ScalaCliCompiler(logger.filter(LogLevel.debug).void, conversion.versions, constants.defaultCacheFolder),
              Duration.Inf,
            )

            val results = run(
              ToScalaJs.next(
                new Phase3Compile(
                  versions                   = conversion.versions,
                  compiler                   = compiler,
                  targetFolder               = c.paths.out,
                  organization               = conversion.organization,
                  publishLocalFolder         = constants.defaultLocalPublishFolder,
                  metadataFetcher            = Npmjs.No,
                  softWrites                 = true,
                  flavour                    = conversion.flavourImpl,
                  generateScalaJsBundlerFile = false,
                  ensureSourceFilesWritten   = true,
                ),
                BuildPhase,
              ),
            )

            val td = System.currentTimeMillis - t0
            logger.warn(td)

            val failures = failuresOf(results)
            if (failures.nonEmpty) reportFailures(failures)
            else {
              val allSuccesses: Map[LibTsSource, PublishedSbtProject] = {
                def go(source: LibTsSource, p: PublishedSbtProject): Map[LibTsSource, PublishedSbtProject] =
                  Map(source -> p) ++ p.project.deps.flatMap { case (k, v) => go(k, v) }

                results.collect { case (s, PhaseRes.Ok(res)) => go(s, res) }.reduceOption(_ ++ _).getOrElse(Map.empty)
              }

              val short: Seq[SbtProject] =
                results
                  .collect { case (_, PhaseRes.Ok(res)) => res.project }
                  .toSeq
                  .filter(_.name != Name.std.unescaped)
                  .sortBy(_.name)

              println()
              println(
                s"Successfully converted ${allSuccesses.keys.map(x => Color.Green(x.libName.value)).mkString(", ")}",
              )
              println("To use in sbt: ")
              println(Color.DarkGray(s"""libraryDependencies ++= Seq(
                   |  ${short.map(p => p.reference.asSbt).mkString("", ",\n  ", "")}
                   |)""".stripMargin))

              0
            }
        }

        exitCode

      case _ =>
        1
    }

}
