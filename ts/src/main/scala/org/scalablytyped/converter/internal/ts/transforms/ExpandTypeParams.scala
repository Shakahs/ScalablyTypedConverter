package org.scalablytyped.converter.internal
package ts
package transforms

import org.scalablytyped.converter.internal.ts.TsTreeScope.LoopDetector

/*
 * This implements the `keyof` and type lookup mechanisms from typescript in a limited context, and brings us a bit closer
 * to being able to enable type bounds in scala.
 *
 *
```typescript
interface C {
  c?: number
}

interface B {
  b: string;
}
interface A extends B {
  a: number;
}
interface Example {
    expandKeyOf<K extends keyof A>(key: K, foo: A[K]): number
    expandUnion<T extends C | B>(bc: T): T
}```

After conversion we'll end up with
```scala
@js.native
trait Example extends js.Object {
  @JSName("expandKeyOf")
  def expandKeyOf_a(key: expandDashTypeDashParametersLib.expandDashTypeDashParametersLibStrings.a, foo: scala.Double): scala.Double = js.native
  @JSName("expandKeyOf")
  def expandKeyOf_b(
    key: expandDashTypeDashParametersLib.expandDashTypeDashParametersLibStrings.b,
    foo: java.lang.String
  ): scala.Double = js.native
  def expandUnion(bc: B): B = js.native
  def expandUnion(bc: C): C = js.native
}
```
 */
object ExpandTypeParams extends TransformMembers with TransformClassMembers {

  override def newClassMembers(scope: TsTreeScope, x: HasClassMembers): IArray[TsMember] =
    x.members.flatMap {
      case m @ TsMemberCall(_, _, sig) =>
        expandTParams(scope / m, sig) match {
          case None           => IArray(m)
          case Some(nonEmpty) => RemoveComment.keepFirstOnly(nonEmpty.map(newSig => m.copy(signature = newSig)))
        }

      case m @ TsMemberFunction(_, _, _, MethodType.Normal, sig, _, _) =>
        expandTParams(scope / m, sig) match {
          case None           => IArray(m)
          case Some(nonEmpty) => RemoveComment.keepFirstOnly(nonEmpty.map(newSig => m.copy(signature = newSig)))
        }

      case other => IArray(other)
    }

  override def newMembers(scope: TsTreeScope, x: TsContainer): IArray[TsContainerOrDecl] =
    x.members.flatMap {
      case m @ TsDeclFunction(_, _, _, sig, _, _) =>
        expandTParams(scope / m, sig) match {
          case None           => IArray(m)
          case Some(nonEmpty) => RemoveComment.keepFirstOnly(nonEmpty.map(newSig => m.copy(signature = newSig)))
        }

      case other => IArray(other)
    }

  /** A signature expands into at most this many overloads; past it, its type parameters are kept as they are. */
  val MaxExpandedSignatures = 200

  /**
    * Each expandable type parameter multiplies the overloads by the same factor whatever signature it is applied to,
    * so the count is known from the substitutions alone, before any signature is rewritten. A signature past the limit
    * is left unexpanded without building the product first: several type parameters with a few variants each make it
    * large enough to exhaust the heap before it could be counted and discarded.
    */
  def expandTParams(scope: TsTreeScope, sig: TsFunSig): Option[IArray[TsFunSig]] = {
    val expansions = sig.tparams.mapNotNone(expandable(scope, sig)).map(exp => (exp, substitutions(scope, exp)))
    val count = expansions.foldLeft(1L) {
      case (n, _) if n > MaxExpandedSignatures => n
      case (n, (exp, subs))                    => n * (subs.length + (if (exp.toKeepInBounds.isDefined) 1 else 0))
    }

    if (count === 0L || count > MaxExpandedSignatures) None
    else
      Some(expansions.foldLeft(IArray(sig)) {
        case (currentSigs, (exp, subs)) => currentSigs.flatMap(expandSignature(exp, subs))
      })
  }

  def expandable(scope: TsTreeScope, sig: TsFunSig)(tp: TsTypeParam): Option[ExpandableTypeParam] = {
    def flatPick(tpe: TsType): IArray[TsType] =
      FollowAliases(scope)(tpe) match {
        case TsTypeUnion(types) => types.flatMap(flatPick)
        case other              => IArray(other)
      }

    lazy val isParam = sig.params.exists(p =>
      p.tpe.exists {
        case TsTypeRef(_, TsQIdent(IArray.exactlyOne(tp.name)), _) => true
        case _                                                     => false
      },
    )

    tp.upperBound.flatMap { bound =>
      flatPick(bound).partitionCollect2(KeyOf, TypeRef(scope)) match {
        case (Distinct(keyOfs), Distinct(typeRefs), Distinct(keepInBounds))
            if keyOfs.nonEmpty || (typeRefs.filterNot(x => OptionalType.undefineds(x.value)).length > 1 && isParam) =>
          Some(ExpandableTypeParam(tp.name, keepInBounds.nonEmptyOpt, keyOfs ++ typeRefs))
        case _ => None
      }
    }
  }

  object Distinct {
    def unapply[T <: AnyRef](ts: IArray[T]): Some[IArray[T]] = Some(ts.distinct)
  }

  final case class ExpandableTypeParam(
      typeParam:      TsIdent,
      toKeepInBounds: Option[IArray[TsType]],
      toExpand:       IArray[Either[TsTypeRef, TsTypeKeyOf]],
  )

  val KeyOf: PartialFunction[TsType, Right[TsTypeRef, TsTypeKeyOf]] = {
    case x @ TsTypeKeyOf(_: TsTypeRef) => Right(x)
  }

  val isAny = Set[TsType](TsTypeRef.any, TsTypeRef.`object`)

  def TypeRef(scope: TsTreeScope): PartialFunction[TsType, Left[TsTypeRef, TsTypeKeyOf]] = {
    case tr: TsTypeRef if tr.tparams.forall(x => !isAny(x)) && !isAny(tr) && !scope.isAbstract(tr.name) => Left(tr)
  }

  /**
    * The rewrites one expandable type parameter is expanded through: one per type its bound names, and one per
    * property of the type a `keyof` bound names. They depend on the type parameter alone, not on the signature.
    */
  def substitutions(scope: TsTreeScope, exp: ExpandableTypeParam): IArray[Map[TsType, TsType]] =
    exp.toExpand.flatMap {
      case Left(tr) =>
        IArray(Map[TsType, TsType](TsTypeRef(exp.typeParam) -> clearCircularRef(exp.typeParam, tr)))

      case Right(TsTypeKeyOf(ref: TsTypeRef)) =>
        AllMembersFor(scope, LoopDetector.initial)(ref).collect {
          case TsMemberProperty(_, _, TsIdentSimple(n), Some(tpe), _, false, _) =>
            Map[TsType, TsType](
              TsTypeRef(exp.typeParam) -> TsTypeLiteral(TsLiteral.Str(n)),
              TsTypeLookup(ref, TsTypeLiteral(TsLiteral.Str(n))) -> tpe,
            )
        }

      case Right(other) =>
        /* enforced in `ExpandableTypes` but not proved */
        sys.error(s"Unexpected: $other")
    }

  def expandSignature(exp: ExpandableTypeParam, substitutions: IArray[Map[TsType, TsType]])(
      sig:                 TsFunSig,
  ): IArray[TsFunSig] = {

    val keptInBounds: Option[TsFunSig] =
      exp.toKeepInBounds.map { types =>
        val rewrittenTparams = sig.tparams.map {
          case tparam @ TsTypeParam(_, exp.typeParam, _, _, _) =>
            tparam.copy(upperBound = Some(TsTypeUnion(types)))
          case other => other
        }

        sig.copy(tparams = rewrittenTparams)
      }

    val sigCleaned = sig.copy(tparams = sig.tparams.filterNot(tp => exp.typeParam === tp.name))

    val expanded = substitutions.map(rewrites => new TypeRewriter(sigCleaned).visitTsFunSig(rewrites)(sigCleaned))

    expanded ++ IArray.fromOption(keptInBounds)
  }

  /**
    * Since we inline the `T` also erase references to it
    * ```typescript
    * <T extends (Array<T> | number)>(t: T): T`
    */
  def clearCircularRef(self: TsIdent, tr: TsTypeRef): TsTypeRef =
    new TypeRewriter(tr).visitTsTypeRef(Map(TsTypeRef(self) -> TsTypeRef.any))(tr)
}
