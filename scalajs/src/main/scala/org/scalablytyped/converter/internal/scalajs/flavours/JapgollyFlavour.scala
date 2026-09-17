package org.scalablytyped.converter.internal
package scalajs
package flavours

import org.scalablytyped.converter.Selection
import org.scalablytyped.converter.internal.scalajs.transforms.Adapter

case class JapgollyFlavour(
    outputPkg:              Name,
    enableLongApplyMethod:  Boolean,
    versions:               Versions,
    enableReactTreeShaking: Selection[Name],
) extends FlavourImplReact {
  override val rewrites      = JapgollyTypeConversions(reactNames, scalaJsDomNames, scalaJsLibNames)
  override val dependencies  = Set(versions.runtime, versions.scalajsReact)
  val memberToPro            = new JapgollyMemberToProp(reactNamesProxy, rewrites)
  val genStBuildingComponent = new JapgollyGenStBuildingComponent(outputPkg, versions.scala)

  final override def rewrittenTree(scope: TreeScope, tree: PackageTree): PackageTree = {
    val parentsResolver    = new ParentsResolver
    val identifyComponents = this.identifyComponents(parentsResolver)
    val findProps          = this.findProps(memberToPro, parentsResolver)
    val genComponents =
      new JapgollyGenComponents(findProps, genStBuildingComponent, reactNamesProxy, enableLongApplyMethod)
    val genCompanions = new GenCompanions(findProps, enableLongApplyMethod) >> GenPromiseOps

    val withCompanions = genCompanions.visitPackageTree(scope)(tree)

    val withComponents: PackageTree =
      if (involvesReact(scope)) {
        val components: IArray[Component] =
          identifyComponents.oneOfEach(scope / withCompanions, withCompanions) ++
            identifyComponents.intrinsics(scope / withCompanions)

        val ret = Adapter(scope)((t, s) => genComponents(s, t, components))(withCompanions)

        if (isReact(scope))
          ret.copy(members = ret.members ++ IArray(genStBuildingComponent.Trait, genStBuildingComponent.Object.tree))
        else ret

      } else withCompanions

    withComponents
  }
}
