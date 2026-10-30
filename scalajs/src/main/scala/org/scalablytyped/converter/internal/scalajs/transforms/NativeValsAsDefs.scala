package org.scalablytyped.converter.internal
package scalajs
package transforms

/**
  * A Scala object holds each of its `val`s in a JVM static field, assigned in the object's static initializer. The
  * JVM caps that initializer at 64KB of bytecode, so an object with thousands of `@js.native` vals cannot be emitted:
  * `lucide-react` exports some 6500 icons from its entry module, and fails with `Method too large: <clinit>`.
  *
  * A parameterless `@js.native def` is a JS getter of the same import, and is no JVM field at all. In the objects
  * large enough to need it, the native vals become such defs; smaller objects keep their stable vals.
  */
object NativeValsAsDefs extends TreeTransformation {
  val MaxNativeVals = 2000

  override def leaveModuleTree(scope: TreeScope)(s: ModuleTree): ModuleTree =
    if (s.isNative || s.members.count(isNativeVal) <= MaxNativeVals) s
    else
      s.copy(members = s.members.map {
        case f: FieldTree if isNativeVal(f) =>
          MethodTree(
            annotations = f.annotations,
            level       = f.level,
            name        = f.name,
            tparams     = Empty,
            params      = Empty,
            impl        = f.impl,
            resultType  = f.tpe,
            isOverride  = f.isOverride,
            comments    = f.comments,
            codePath    = f.codePath,
            isImplicit  = false,
          )
        case other => other
      })

  private def isNativeVal(tree: Tree): Boolean =
    tree match {
      case f: FieldTree => f.isReadOnly && !f.isOverride && f.isNative
      case _ => false
    }
}
