package org.scalablytyped.converter.internal.importer

import com.olvind.logging.Logger
import org.scalablytyped.converter.internal.phases.{GetDeps, IsCircular, Phase, PhaseRes}
import org.scalablytyped.converter.internal.scalajs.flavours.FlavourImpl
import org.scalablytyped.converter.internal.scalajs.{Name, ParentsResolver, TreeScope, Versions}
import org.scalablytyped.converter.internal.scalajs.transforms.{AlignOverriddenWildcards, AnyArgsToWildcards, Mangler, SetPrivateWithin, Sorter}

import scala.collection.immutable.SortedSet

class PhaseFlavour(flavour: FlavourImpl, maybePrivateWithin: Option[Name], scalaVersion: Versions.Scala)
    extends Phase[LibTsSource, LibScalaJs, LibScalaJs] {

  override def apply(
      source:     LibTsSource,
      lib:        LibScalaJs,
      getDeps:    GetDeps[LibTsSource, LibScalaJs],
      isCircular: IsCircular,
      _logger:    Logger[Unit],
  ): PhaseRes[LibTsSource, LibScalaJs] = {
    val logger = _logger.withContext("flavour", flavour.toString)

    getDeps(SortedSet.empty[LibTsSource] ++ (lib.dependencies.keys: Iterable[LibTsSource])).map { deps =>
      val originalScope = new TreeScope.Root(
        libName       = lib.scalaName,
        _dependencies = LibScalaJs.resolveDependencyTrees(lib.dependencies),
        logger        = logger,
        pedantic      = false,
        outputPkg     = flavour.outputPkg,
      )

      val tree0 = lib.packageTree
      val tree1 = flavour.rewrittenTree(originalScope, tree0)
      // after the flavour, which copies member types into varargs and lambda parameters, where `?` is not a type
      /* Ancestors as they are written out: the dependencies after this phase, with their wildcards. */
      val flavouredScope = new TreeScope.Root(
        libName       = lib.scalaName,
        _dependencies = LibScalaJs.resolveDependencyTrees(deps),
        logger        = logger,
        pedantic      = false,
        outputPkg     = flavour.outputPkg,
      )
      val tree1b =
        if (scalaVersion.is3)
          new AlignOverriddenWildcards(new ParentsResolver)
            .visitPackageTree(flavouredScope)(AnyArgsToWildcards.visitPackageTree(originalScope)(tree1))
        else tree1
      val tree2 = Mangler.visitPackageTree(originalScope)(tree1b)
      val tree3 = maybePrivateWithin.foldLeft(tree2) {
        case (tree, privateWithin) => new SetPrivateWithin(privateWithin).visitPackageTree(originalScope)(tree)
      }
      val tree4 = Sorter.visitPackageTree(originalScope)(tree3)

      LibScalaJs(lib.source)(
        libName      = lib.libName,
        scalaName    = lib.scalaName,
        libVersion   = lib.libVersion,
        packageTree  = tree4,
        dependencies = deps,
        isStdLib     = lib.isStdLib,
        names        = lib.names,
      )
    }
  }
}
