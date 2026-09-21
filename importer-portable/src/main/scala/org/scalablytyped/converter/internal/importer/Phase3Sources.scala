package org.scalablytyped.converter.internal
package importer

import com.olvind.logging.Logger
import org.scalablytyped.converter.internal.phases.{GetDeps, IsCircular, Phase, PhaseRes}
import org.scalablytyped.converter.internal.scalajs._
import org.scalablytyped.converter.internal.scalajs.flavours.FlavourImpl

import scala.collection.immutable.SortedSet

/**
  * The last phase, for builds that compile the generated facades themselves.
  *
  * [[Phase3Compile]] goes all the way from the Scala AST to published artifacts: it writes
  * the sources, resolves a Scala compiler over the network, compiles them, and publishes
  * jars into the user's local ivy repository. That is the right shape for a one-off run on
  * a developer's machine, and the wrong one for a build system that already has a pinned
  * compiler and a dependency graph of its own — there it means an action that reaches the
  * network, writes outside its own output directory, and produces artifacts whose versions
  * are digests no caller can predict.
  *
  * This phase stops at the sources. What it writes is a function of the TypeScript it read
  * and the conversion options, and nothing else, so the same inputs give the same bytes and
  * the calling build can compile them with its own toolchain.
  */
class Phase3Sources(
    versions:     Versions,
    targetFolder: os.Path,
    flavour:      FlavourImpl,
) extends Phase[LibTsSource, LibScalaJs, GeneratedSources] {

  override def apply(
      source:  LibTsSource,
      lib:     LibScalaJs,
      getDeps: GetDeps[LibTsSource, GeneratedSources],
      v4:      IsCircular,
      _logger: Logger[Unit],
  ): PhaseRes[LibTsSource, GeneratedSources] = {
    val logger = _logger.withContext("flavour", flavour.toString)

    /* Not for anything this phase writes — a library's own package tree carries what it
     * needs. It is what makes the phase runner generate the dependencies too, so one
     * requested library brings the whole closure it is written against. */
    getDeps(SortedSet.empty[LibTsSource] ++ lib.dependencies.keys.map(x => x: LibTsSource)).map { deps =>
      val scope = new TreeScope.Root(
        libName       = lib.scalaName,
        _dependencies = lib.dependencies.map { case (_, lib) => lib.scalaName -> lib.packageTree },
        logger        = logger,
        pedantic      = false,
        outputPkg     = flavour.outputPkg,
      )

      val (scalaFiles, printMs) =
        timed(Printer(scope, new ParentsResolver, lib.packageTree, flavour.outputPkg, versions.scala))
      logger.warn(s"printed ${scalaFiles.length} scala files in $printMs ms")

      /* One directory per library, so a library's files never collide with another's and
       * the layout is the same whichever subset of libraries a run was asked for. */
      val libraryFolder = targetFolder / lib.libName

      val ((), syncMs) = timed(files.sync(scalaFiles, libraryFolder, deleteUnknowns = true, soft = true))
      logger.warn(s"synced sources to $libraryFolder in $syncMs ms")

      GeneratedSources(lib.libName, libraryFolder, deps.toMap)
    }
  }
}

/** Where one library's generated Scala sources were written. */
case class GeneratedSources(
    libName: String,
    folder:  os.Path,
    deps:    Map[LibTsSource, GeneratedSources],
)
