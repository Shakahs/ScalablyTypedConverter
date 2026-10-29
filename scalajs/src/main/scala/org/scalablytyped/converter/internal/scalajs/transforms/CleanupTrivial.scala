package org.scalablytyped.converter.internal
package scalajs
package transforms

/**
  * This is the second part of a two step process to rid ourselves of the myriad of
  *  type aliases resulting from the resolution of modules.
  *
  *  The first part is done in typescript, `InlineTrivialTypeAlias`, where we inline all trivial
  *  type aliases. We need to do the removal here in scala.js to ensure that all dependencies
  *  can also resolve all their uses of the intermediate type aliases.
  *
  *  The library's entry module (`typings.<lib>.mod`) keeps its trivial type aliases. Those are
  *  the types the package exports from its entry point, so they are its public names; the
  *  declarations they point to may sit in files a bundler names by content hash, which change
  *  from one release to the next.
  */
object CleanupTrivial extends TreeTransformation {
  override def leaveModuleTree(scope: TreeScope)(s: ModuleTree): ModuleTree =
    s.copy(members = clearEmptyContainers(removeTrivial(s.members, isEntryModule(s.codePath))))

  override def leavePackageTree(scope: TreeScope)(s: PackageTree): PackageTree =
    s.copy(members = clearEmptyContainers(removeTrivial(s.members, isEntryModule(s.codePath))))

  def isEntryModule(codePath: QualifiedName): Boolean =
    codePath.parts.length === 3 && codePath.parts.last === Name.mod

  def clearEmptyContainers(members: IArray[Tree]): IArray[Tree] =
    members.filter {
      case x: ModuleTree =>
        x.parents.nonEmpty ||
          x.members.nonEmpty ||
          x.name === Name.namespaced ||
          x.comments.has[Marker.ManglerLeaveAlone.type] // keeps the require object and hence the whole library if empty
      case x: PackageTree =>
        x.members.nonEmpty
      case _ => true
    }

  def removeTrivial(members: IArray[Tree], keepTypeAliases: Boolean): IArray[Tree] =
    members.filterNot {
      case _: TypeAliasTree if keepTypeAliases => false
      case x => x.comments.has[Marker.IsTrivial.type]
    }
}
