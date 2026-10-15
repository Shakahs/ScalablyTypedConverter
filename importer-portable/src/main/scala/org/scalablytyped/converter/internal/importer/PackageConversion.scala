package org.scalablytyped.converter.internal
package importer

import com.olvind.logging.Logger
import org.scalablytyped.converter.internal.phases.{GetDeps, PhaseRes}
import org.scalablytyped.converter.internal.ts.CalculateLibraryVersion.PackageJsonOnly
import org.scalablytyped.converter.internal.ts.{TsIdent, TsIdentLibrary}

import scala.collection.immutable.SortedMap
import scala.collection.mutable

/**
  * Converts one library, given the interfaces of the libraries it reaches, and writes its sources and its own
  * interface.
  *
  * The whole-tree conversion runs every phase for every library it reaches. This runs them for the one library only:
  * a phase asks for its dependencies' results, and gets them from their interfaces, so nothing is converted twice
  * however many libraries are written against it, and a library's output depends on its own files and its
  * dependencies' interfaces alone. The steps of a build are what order the libraries.
  */
object PackageConversion {

  /**
    * @param libName      the library, as the converter names it (`std` for the TypeScript standard library)
    * @param folder       the package holding its declarations; for `std`, the `typescript` package
    * @param dependencies interfaces of every library it reaches, its own dependencies' first
    * @param sources      where its Scala sources are written, in a folder named after it
    * @param interface    where its interface is written
    */
  def apply(
      conversion:   ConversionOptions,
      libName:      TsIdentLibrary,
      folder:       os.Path,
      dependencies: IArray[os.Path],
      sources:      os.Path,
      interface:    os.Path,
      parallelism:  Int,
      logger:       Logger[Unit],
  ): Either[String, PackageInterface] = {
    /* How long each step took, logged as one line for the build to aggregate. */
    val timings = mutable.LinkedHashMap.empty[String, Long]
    def step[T](name: String)(run: => T): T = {
      val (result, ms) = timed(run)
      timings(name) = ms
      result
    }

    /* A library reached at more than one version is converted against the first named. */
    val converted: IArray[PackageInterface.Converted] = step("read") {
      IArray.fromTraversable(
        dependencies.toList.distinct
          .map(PackageInterface.read)
          .collect { case c: PackageInterface.Converted => c }
          .distinctBy(_.libName),
      )
    }

    val own: LibTsSource =
      if (libName === TsIdent.std)
        LibTsSource.StdLibSource(
          InFolder(folder / "lib"),
          IArray.fromTraversable(conversion.stdLibs.map(s => InFile(folder / "lib" / s"lib.$s.d.ts"))),
          TsIdent.std,
        )
      else LibTsSource.FromFolder(InFolder(folder), libName)

    val known: Map[TsIdentLibrary, LibTsSource] =
      converted.map(c => c.libName -> PackageInterface.placeholderSource(c.libName)).toMap.updated(libName, own)

    val linked = new PackageInterface.Linked(converted, known)

    val stdLib: LibTsSource.StdLibSource =
      known.get(TsIdent.std) match {
        case Some(std: LibTsSource.StdLibSource) => std
        case _                                   => return Left("The interface of std must be given, or std converted")
      }

    val resolver = new LibraryResolver(
      stdLib,
      IArray.fromTraversable(known.values.collect { case f: LibTsSource.FromFolder => f }),
      conversion.ignoredLibs,
    )

    /* A phase asks for its dependencies by source; each is answered from its interface. */
    def fromInterfaces[T](result: TsIdentLibrary => T): GetDeps[LibTsSource, T] =
      wanted =>
        PhaseRes.Ok(
          SortedMap.from(wanted.toList.collect { case source if linked.contains(source) => source -> result(source.libName) }),
        )

    val phase1 = new Phase1ReadTypescript(
      resolve                 = resolver,
      calculateLibraryVersion = PackageJsonOnly,
      ignored                 = conversion.ignoredLibs,
      ignoredModulePrefixes   = conversion.ignoredModulePrefixes,
      pedantic                = false,
      parser                  = PersistingParser(None, IArray(own.folder), logger.void),
      parseParallelism        = parallelism,
      expandTypeMappings      = conversion.expandTypeMappings,
    )
    val phase2 = new Phase2ToScalaJs(
      pedantic                 = false,
      scalaVersion             = conversion.versions.scala,
      enableScalaJsDefined     = conversion.enableScalaJsDefined,
      outputPkg                = conversion.outputPackage,
      flavour                  = conversion.flavourImpl,
      useDeprecatedModuleNames = conversion.useDeprecatedModuleNames,
    )
    val flavour = new PhaseFlavour(
      conversion.flavourImpl,
      maybePrivateWithin = conversion.privateWithin,
      scalaVersion       = conversion.versions.scala,
    )
    val phase3 = new Phase3Sources(conversion.versions, sources, conversion.flavourImpl)

    val result: PhaseRes[LibTsSource, PackageInterface] =
      for {
        ts <- step("ts")(phase1(own, own, fromInterfaces(linked.ts), false, logger))
        scala <- step("scala")(phase2(own, ts, fromInterfaces(linked.scala), false, logger))
        flavoured <- step("flavour")(flavour(own, scala, fromInterfaces(linked.flavour), false, logger))
        _ <- step("print")(
          phase3(
            own,
            flavoured,
            fromInterfaces(name => GeneratedSources(name.`__value`, sources / name.`__value`, Map.empty)),
            false,
            logger,
          ),
        )
      } yield PackageInterface.Converted(
        libName             = libName,
        isStdLib            = scala.isStdLib,
        tsVersion           = ts.version,
        parsed              = ts.parsed,
        tsDependencies      = IArray.fromTraversable(ts.dependencies.keys.map(_.libName)),
        scalaLibName        = scala.libName,
        scalaName           = scala.scalaName,
        scalaVersion        = scala.libVersion,
        scalaTree           = scala.packageTree,
        scalaDependencies   = IArray.fromTraversable(scala.dependencies.keys.toList.sortBy(_.libName.value).map(_.libName)),
        names               = scala.names.own,
        flavourTree         = flavoured.packageTree,
        flavourDependencies = IArray.fromTraversable(flavoured.dependencies.keys.toList.sortBy(_.libName.value).map(_.libName)),
      )

    val written = result match {
      case PhaseRes.Ok(written) =>
        step("write")(PackageInterface.write(interface, written))
        Right(written)
      case PhaseRes.Ignore() =>
        val absent = PackageInterface.Absent(libName)
        step("write")(PackageInterface.write(interface, absent))
        Right(absent)
      case PhaseRes.Failure(errors) =>
        Left(errors.values.map(_.fold(e => s"${e.getClass.getName}: ${e.getMessage}", identity)).mkString("\n"))
    }
    logger.warn(
      s"timing library=${libName.value} interfaces=${converted.length} " +
        timings.map { case (name, ms) => s"$name=$ms" }.mkString(" "),
    )
    written
  }
}
