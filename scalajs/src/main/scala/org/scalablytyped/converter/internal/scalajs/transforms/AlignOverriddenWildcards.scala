package org.scalablytyped.converter.internal
package scalajs
package transforms

/**
  * Gives a member the `Any` arguments of the member it overrides, where [[AnyArgsToWildcards]] left them.
  *
  * A parent type cannot take a wildcard argument, so `extends ICellRenderer[Any]` keeps `Any`, and every member
  * inherited through it has `Any` where its type parameter was. A member overriding one of those was rewritten on its
  * own, and has `?` there:
  *
  * ```scala
  * trait ICellRenderer[TData] { def refresh(params: ICellRendererParams[TData, ?]): Boolean }
  * class CheckboxCellRenderer extends ICellRenderer[Any] {
  *   override def refresh(params: ICellRendererParams[?, ?]): Boolean // must be ICellRendererParams[Any, ?]
  * }
  * ```
  *
  * An override's parameters must have exactly the overridden types, so wherever one has `?` and an overridden member
  * has `Any`, it takes `Any`. Run on the tree [[AnyArgsToWildcards]] rewrote, so the parents found are rewritten too.
  */
class AlignOverriddenWildcards(parentsResolver: ParentsResolver) extends TreeTransformation {

  override def leaveClassTree(scope: TreeScope)(cls: ClassTree): ClassTree =
    if (cls.parents.isEmpty) cls else cls.copy(members = alignMembers(scope, cls, cls.members))

  override def leaveModuleTree(scope: TreeScope)(mod: ModuleTree): ModuleTree =
    if (mod.parents.isEmpty) mod else mod.copy(members = alignMembers(scope, mod, mod.members))

  private def alignMembers(scope: TreeScope, tree: InheritanceTree, members: IArray[Tree]): IArray[Tree] = {
    val inherited: Map[Name, IArray[Tree]] =
      IArray
        .fromTraversable(parentsResolver(scope, tree).transitiveParents.values)
        .flatMap(_.members)
        .groupBy(_.name)

    members.map {
      case field: FieldTree =>
        val overridden = inherited.getOrElse(field.name, Empty).collect { case f: FieldTree => f.tpe }
        field.copy(tpe = overridden.foldLeft(field.tpe)(alignTo))

      case method: MethodTree =>
        val overridden = inherited.getOrElse(method.name, Empty).collect {
          case m: MethodTree if sameShape(method, m) => m
        }
        overridden.foldLeft(method) { (acc, parent) =>
          val params = acc.params.zip(parent.params).map {
            case (own, theirs) => own.zip(theirs).map { case (p, q) => p.copy(tpe = alignTo(p.tpe, q.tpe)) }
          }
          acc.copy(params = params, resultType = alignTo(acc.resultType, parent.resultType))
        }

      case other => other
    }
  }

  private def sameShape(one: MethodTree, two: MethodTree): Boolean =
    one.tparams.length == two.tparams.length &&
      one.params.length == two.params.length &&
      one.params.zip(two.params).forall { case (a, b) => a.length == b.length }

  /* `own` with `Any` wherever it has `?` and `overridden` has `Any`, if the two are otherwise the same type */
  private def alignTo(own: TypeRef, overridden: TypeRef): TypeRef =
    align(own, overridden).getOrElse(own)

  private def align(own: TypeRef, overridden: TypeRef): Option[TypeRef] =
    if (isWildcard(own) && isAny(overridden)) Some(overridden)
    else if (isAny(own) && isWildcard(overridden)) Some(own)
    /* a result narrowed to `A & B` overrides `A` */
    else if (own.typeName === QualifiedName.INTERSECTION && !(overridden.typeName === QualifiedName.INTERSECTION))
      Some(own.copy(targs = own.targs.map(part => align(part, overridden).getOrElse(part))))
    else if (own.typeName === overridden.typeName && own.targs.length == overridden.targs.length) {
      val aligned = own.targs.zip(overridden.targs).map { case (a, b) => align(a, b) }
      if (aligned.forall(_.isDefined)) Some(own.copy(targs = aligned.map(_.get))) else None
    } else None

  private def isAny(tr: TypeRef): Boolean =
    tr.typeName === QualifiedName.Any && tr.targs.isEmpty

  private def isWildcard(tr: TypeRef): Boolean =
    tr.typeName === QualifiedName.WILDCARD
}
