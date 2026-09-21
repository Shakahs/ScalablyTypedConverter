package org.scalablytyped.converter.internal
package importer

import org.scalablytyped.converter.internal.importer.LibTsSource.{FromFolder, StdLibSource}
import org.scalablytyped.converter.internal.seqs._
import org.scalablytyped.converter.internal.ts.{TsIdent, TsIdentLibrary, TsIdentLibraryScoped}

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet

object Bootstrap {

  /**
    * At this point we have scanned node_modules (and DT, if in CI) and know the location ([[LibTsSource]])
    *  of all libraries it's possible to import.
    *
    * @param initialLibs determines if all the libraries explicitly wanted by the user were available
    */
  case class Bootstrapped(
      inputFolders:    IArray[InFolder],
      libraryResolver: LibraryResolver,
      initialLibs:     Either[Unresolved, Vector[LibTsSource]],
  )

  def forCi(
      externalsFolder: InFolder,
      dtFolder:        InFolder,
      conversion:      ConversionOptions,
      wantedLibs:      SortedSet[TsIdentLibrary],
  ): Bootstrapped = {
    val stdLibSource: StdLibSource = {
      val folder = externalsFolder.path / "typescript" / "lib"

      require(files.exists(folder), s"You must add typescript as a dependency. $folder must exist.")
      require(!conversion.ignoredLibs.contains(TsIdent.std), "You cannot ignore std")

      StdLibSource(
        InFolder(folder),
        IArray.fromTraversable(conversion.stdLibs.map(s => InFile(folder / s"lib.$s.d.ts"))),
        TsIdent.std,
      )
    }

    val inputFolders = IArray(externalsFolder, dtFolder)
    val allSources   = findSources(inputFolders)

    val libraryResolver = new LibraryResolver(stdLibSource, allSources, conversion.ignoredLibs)

    val initial: Either[Unresolved, Vector[LibTsSource]] =
      wantedLibs match {
        case sets.EmptySet() => Right(allSources.toVector)
        case wantedLibs      => resolveAll(libraryResolver, wantedLibs)
      }

    Bootstrapped(inputFolders, libraryResolver, initial)
  }

  def fromNodeModules(
      fromFolder: InFolder,
      conversion: ConversionOptions,
      wantedLibs: SortedSet[TsIdentLibrary],
  ): Bootstrapped = {
    val stdLibSource = {
      val folder = fromFolder.path / "typescript" / "lib"

      require(files.exists(folder), s"You must add typescript as a dependency. $folder must exist.")
      require(!conversion.ignoredLibs.contains(TsIdent.std), "You cannot ignore std")

      StdLibSource(
        InFolder(folder),
        IArray.fromTraversable(conversion.stdLibs.map(s => InFile(folder / s"lib.$s.d.ts"))),
        TsIdent.std,
      )
    }

    val `@types`: Option[InFolder] =
      fromFolder.path / "@types" match {
        case dir if os.isDir(dir) => Some(InFolder(dir))
        case _                    => None
      }

    val inputFolders: IArray[InFolder] =
      IArray.fromOptions(`@types`, Some(fromFolder))

    val allSources: IArray[LibTsSource.FromFolder] =
      findSources(inputFolders)

    val libraryResolver = new LibraryResolver(stdLibSource, allSources, conversion.ignoredLibs)

    val initialLibs: Either[Unresolved, Vector[LibTsSource]] =
      resolveAll(libraryResolver, wantedLibs)

    Bootstrapped(inputFolders, libraryResolver, initialLibs)
  }

  /**
    * Every library that can be converted, starting from the given folders and following
    * each package back to the `node_modules` directory it really lives in.
    *
    * A flat scan of one directory only finds every library when the package manager has
    * hoisted the whole dependency graph into it. pnpm does not: a package in
    * `node_modules` is a symlink into a virtual store, `<store>/<name>@<version>/node_modules/<name>`,
    * and its own dependencies are that directory's siblings rather than entries of the
    * top-level folder. A scan that stops at the top level therefore sees the packages that
    * were asked for and none of what they are written against, so every type a package
    * re-exports from a dependency silently fails to resolve and the generated facade is
    * missing the members that came from it.
    *
    * Following the symlink and scanning the enclosing `node_modules` finds those
    * dependencies wherever the package manager put them. It is the same step for the
    * classic layouts: under a hoisted `node_modules` the enclosing directory is the one
    * already being scanned, and under a nested one it is the parent package's.
    *
    * Sources are still deduplicated by library name, first found winning, so the packages
    * the input folders name directly keep their identity — and their version — over a copy
    * reached through some other package's dependencies.
    */
  def findSources(folders: IArray[InFolder]): IArray[LibTsSource.FromFolder] = {
    @tailrec
    def go(
        pending: List[InFolder],
        visited: Set[os.Path],
        found:   IArray[LibTsSource.FromFolder],
    ): IArray[LibTsSource.FromFolder] =
      pending match {
        case Nil => found
        case next :: rest =>
          val real = realPath(next.path)
          if (visited(real) || !os.isDir(next.path)) go(rest, visited, found)
          else {
            val sources    = forFolder(next)
            val foundNames = found.map(_.libName).toSet
            val newSources = sources.filterNot(s => foundNames(s.libName))

            /* `forFolder` leaves `@types` to its own input folder, so each newly reached
             * `node_modules` contributes its own. */
            val scoped    = InFolder(next.path / "@types")
            val enclosing = sources.mapNotNone(enclosingNodeModules).toList

            go(rest ::: scoped :: enclosing, visited + real, found ++ newSources)
          }
      }

    go(folders.toList, Set.empty, IArray.Empty)
  }

  /** The `node_modules` directory a package actually lives in, following symlinks. */
  private def enclosingNodeModules(source: LibTsSource.FromFolder): Option[InFolder] = {
    val real = realPath(source.folder.path)
    val enclosing = source.libName match {
      /* `@scope/name` sits one directory deeper, in the scope directory. */
      case _: TsIdentLibraryScoped => real / os.up / os.up
      case _                       => real / os.up
    }
    if (enclosing.last == "node_modules" && os.isDir(enclosing)) Some(InFolder(enclosing)) else None
  }

  private def realPath(path: os.Path): os.Path =
    if (os.exists(path)) os.Path(path.toNIO.toRealPath()) else path

  private def forFolder(folder: InFolder): IArray[LibTsSource.FromFolder] =
    IArray.fromTraversable(
      os.list(folder.path)
        .collect { case dir if os.isDir(dir) => dir }
        .flatMap {
          case path if path.last.startsWith("@") =>
            if (path.last.startsWith("@types")) Nil
            else
              os.list(path)
                .collect { case dir if os.isDir(dir) => dir }
                .map(nestedPath => FromFolder(InFolder(nestedPath), TsIdentLibrary(s"${path.last}/${nestedPath.last}")))
          case path => List(FromFolder(InFolder(path), TsIdentLibrary(path.last)))
        }
        .filter(s => LibTsSource.hasTypescriptSources(s.folder)),
    )

  def resolveAll(
      libraryResolver: LibraryResolver,
      libs:            SortedSet[TsIdentLibrary],
  ): Either[Unresolved, Vector[LibTsSource]] =
    libs.toVector
      .map(libraryResolver.library)
      .partitionCollect2({ case LibraryResolver.Found(x) => x }, { case LibraryResolver.NotAvailable(name) => name }) match {
      case (allFound, notAvailable, _) if notAvailable.isEmpty => Right(allFound.toVector)
      case (_, notAvailable, _)                                => Left(Unresolved(notAvailable.toVector))
    }

  case class Unresolved(notAvailable: Vector[TsIdentLibrary]) {
    def msg =
      s"Missing typescript definitions for the following libraries: ${notAvailable.map(_.value).mkString(", ")}. Try to add a corresponding `@types` npm package, or use `stIgnore` to ignore"
  }
}
