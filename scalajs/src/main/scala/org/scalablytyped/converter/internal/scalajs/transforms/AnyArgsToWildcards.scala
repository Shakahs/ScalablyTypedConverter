package org.scalablytyped.converter.internal
package scalajs
package transforms

/**
  * Typescript's `any` as a type argument, written out or filled in from a defaulted type parameter, means that
  * argument is not known. `Any` says something else in Scala: an invariant `ColDef[TData, Any]` is not a supertype of
  * `ColDef[TData, String]`, so code using the facade has to write `Any` itself and lose the type.
  *
  * ```typescript
  * interface GridOptions<TData = any> { columnDefs?: ColDef<TData>[] } // ColDef<TData = any, TValue = any>
  * ```
  *
  * Rewritten into
  * ```scala
  * trait GridOptions[TData] { var columnDefs: js.UndefOr[js.Array[ColDef[TData, ?]]] }
  * ```
  *
  * Scala 3 accepts a wildcard argument only where it applies to a class or trait, so `Any` is kept as the argument of
  * a type alias (`js.UndefOr` among them), of a parent type and inside an expression other than a lambda's parameter
  * types. A function type keeps it too: an `any` parameter does accept anything, and a lambda cannot declare a
  * wildcard parameter.
  *
  * This runs after the flavour, which copies member types into varargs and lambda parameters.
  */
object AnyArgsToWildcards extends TreeTransformation {
  private val FunctionName = "(This)?Function\\d+".r

  override def leaveTypeRef(scope: TreeScope)(s: TypeRef): TypeRef =
    if (s.targs.exists(isAny) && !isFunction(s.typeName) && namesClass(scope, s.typeName) && !inParentOrExpr(scope))
      s.copy(targs = s.targs.map(targ => if (isAny(targ)) TypeRef.Wildcard.withComments(targ.comments) else targ))
    else s

  private def isAny(tr: TypeRef): Boolean =
    tr.typeName === QualifiedName.Any && tr.targs.isEmpty

  private def isFunction(name: QualifiedName): Boolean =
    name.parts.nonEmpty && FunctionName.matches(name.parts.last.unescaped) && {
      val owner = QualifiedName(name.parts.init)
      owner === QualifiedName.scala_js || owner === QualifiedName.scala
    }

  private def namesClass(scope: TreeScope, name: QualifiedName): Boolean =
    scope.lookup(name).exists {
      case (_: ClassTree, _) => true
      case _ => false
    }

  /* The stack starts with this type reference and any it is an argument of. A lambda's parameter is typed like the
   * member the lambda is assigned to, so its arguments are rewritten as the member's are: the flavour's builders pass
   * the lambda where the member's type is expected. */
  private def inParentOrExpr(scope: TreeScope): Boolean = {
    val owner = scope.stack.dropWhile {
      case _: TypeRef => true
      case _ => false
    }
    val inExpr = owner.exists {
      case _: ExprTree => true
      case _ => false
    }
    owner.headOption match {
      case Some(_: InheritanceTree) => true
      case Some(_: ParamTree)       => inExpr && !owner.lift(1).exists(_.isInstanceOf[ExprTree.Lambda])
      case _ => inExpr
    }
  }
}
