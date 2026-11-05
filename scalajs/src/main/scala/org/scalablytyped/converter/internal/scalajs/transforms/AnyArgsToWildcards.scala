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
  * Scala 3 reduces a wildcard argument of a type alias only where the alias's parameter appears once, directly as an
  * argument of a class (`TypeApplications.Reducer`); anything else is an unreducible application. An alias of the
  * library's own given `any` arguments is therefore expanded where it is used, down to the classes its body names, and
  * each of those classes gets the wildcard instead:
  *
  * ```typescript
  * type ColumnDef<TData, TValue> = DisplayColumnDef<TData, TValue> | AccessorColumnDef<TData, TValue>
  * interface TableOptions<TData> { columns: ColumnDef<TData, any>[] }
  * ```
  *
  * Rewritten into
  * ```scala
  * trait TableOptions[TData] { var columns: js.Array[DisplayColumnDef[TData, ?] | AccessorColumnDef[TData, ?]] }
  * ```
  * (each of those aliases expanded in turn). An alias whose expansion yields no wildcard keeps its `Any` argument, as
  * does an alias it cannot see (`js.UndefOr` among them). `Any` is also kept as the argument of a parent type and
  * inside an expression other than a lambda's parameter types. A function type keeps it too: an `any` parameter does
  * accept anything, and a lambda cannot declare a wildcard parameter.
  *
  * This runs after the flavour, which copies member types into varargs and lambda parameters.
  */
object AnyArgsToWildcards extends TreeTransformation {
  private val FunctionName = "(This)?Function\\d+".r

  override def leaveTypeRef(scope: TreeScope)(s: TypeRef): TypeRef =
    if (!s.targs.exists(isAny) || isFunction(s.typeName) || inParentOrExpr(scope)) s
    else if (namesClass(scope, s.typeName)) wildcardAnyArgs(s)
    else expandAlias(scope, s, Set.empty).filter(containsWildcard).getOrElse(s)

  private def wildcardAnyArgs(tr: TypeRef): TypeRef =
    tr.copy(targs = tr.targs.map(targ => if (isAny(targ)) TypeRef.Wildcard.withComments(targ.comments) else targ))

  /* The body of the alias `ref` names, its parameters filled in by `ref`'s arguments, with every class it reaches given
   * wildcards for its `Any` arguments. `visited` holds the aliases already being expanded, so a recursive alias stops. */
  private def expandAlias(scope: TreeScope, ref: TypeRef, visited: Set[QualifiedName]): Option[TypeRef] =
    if (visited(ref.typeName)) None
    else
      scope
        .lookup(ref.typeName)
        .collectFirst { case (alias: TypeAliasTree, _) if alias.tparams.length === ref.targs.length => alias }
        .map(alias =>
          wildcardExpanded(scope, FillInTParams(alias, scope, ref.targs, Empty).alias, visited + ref.typeName),
        )

  /* Unions and intersections are type references to internal names, so their members are rewritten as arguments are,
   * without rebuilding (and so recombining) them. */
  private def wildcardExpanded(scope: TreeScope, tpe: TypeRef, visited: Set[QualifiedName]): TypeRef =
    if (isFunction(tpe.typeName)) tpe
    else {
      val rewritten = tpe.copy(targs = tpe.targs.map(targ => wildcardExpanded(scope, targ, visited)))
      if (!rewritten.targs.exists(isAny)) rewritten
      else if (namesClass(scope, rewritten.typeName)) wildcardAnyArgs(rewritten)
      else expandAlias(scope, rewritten, visited).getOrElse(rewritten)
    }

  private def containsWildcard(tr: TypeRef): Boolean =
    tr.typeName === TypeRef.Wildcard.typeName || tr.targs.exists(containsWildcard)

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
