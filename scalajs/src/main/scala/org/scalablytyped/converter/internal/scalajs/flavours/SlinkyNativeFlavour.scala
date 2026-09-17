package org.scalablytyped.converter.internal
package scalajs
package flavours

import org.scalablytyped.converter.Selection
import org.scalablytyped.converter.internal.scalajs.transforms.Adapter

case class SlinkyNativeFlavour(
    outputPkg:              Name,
    enableLongApplyMethod:  Boolean,
    versions:               Versions,
    enableReactTreeShaking: Selection[Name],
) extends FlavourImplReact {

  override val dependencies: Set[Dep] =
    Set(versions.runtime, versions.slinkyNative, versions.scalaJsDom)

  override val rewrites: IArray[CastConversion] =
    SlinkyTypeConversions(scalaJsDomNames, scalaJsLibNames, reactNames, isWeb = false)

  val memberToProp           = new MemberToProp.Default(rewrites)
  val genStBuildingComponent = new SlinkyGenStBuildingComponent(outputPkg, versions.scala)

  final override def rewrittenTree(scope: TreeScope, tree: PackageTree): PackageTree = {
    val parentsResolver    = new ParentsResolver
    val identifyComponents = this.identifyComponents(parentsResolver)
    val findProps          = this.findProps(memberToProp, parentsResolver)
    val gen =
      new SlinkyGenComponents(SlinkyGenComponents.Native(()), findProps, genStBuildingComponent, reactNamesProxy)
    val genCompanions = new GenCompanions(findProps, enableLongApplyMethod) >> GenPromiseOps

    val withCompanions = genCompanions.visitPackageTree(scope)(tree)

    val withComponents = if (involvesReact(scope)) {
      val components: IArray[Component] =
        identifyComponents.oneOfEach(scope / withCompanions, withCompanions)

      val ret = Adapter(scope)((t, s) => gen(s, t, components))(withCompanions)

      if (isReact(scope))
        ret.copy(members = ret.members ++ IArray(genStBuildingComponent.Trait, genStBuildingComponent.Object.tree))
      else ret

    } else withCompanions

    withComponents
  }
}
