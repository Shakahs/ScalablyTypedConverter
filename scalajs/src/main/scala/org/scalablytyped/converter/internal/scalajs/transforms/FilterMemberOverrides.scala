package org.scalablytyped.converter.internal
package scalajs
package transforms

/**
  * We filter away unneeded overrides, since they add nothing, and
  * wreck havoc with IDE performance.
  *
  * For fields with name clashes and different types, we rename them.
  * For methods with similar / clashing signatures, we also rename.
  *
  * Note that no subtype calculation is done for now.
  */
class FilterMemberOverrides(erasure: Erasure, parentsResolver: ParentsResolver) extends TreeTransformation {

  override def leaveClassTree(scope: TreeScope)(s: ClassTree): ClassTree =
    s.copy(members = newMembers(scope, s, s.members, s.parents))

  override def leaveModuleTree(scope: TreeScope)(s: ModuleTree): ModuleTree =
    s.copy(members = newMembers(scope, s, s.members, s.parents))

  override def leavePackageTree(scope: TreeScope)(s: PackageTree): PackageTree =
    s.copy(members = newMembers(scope, s, s.members, Empty))

  private def newMembers(
      scope:       TreeScope,
      owner:       Tree with HasAnnotations,
      members:     IArray[Tree],
      inheritance: IArray[TypeRef],
  ): IArray[Tree] = {
    val (methods, fields, modules, other) = members.partitionCollect3(
      { case x: MethodTree => x },
      { case x: FieldTree  => x },
      { case x: ModuleTree => x },
    )
    val methodsByName: Map[Name, IArray[MethodTree]] =
      methods.groupBy(_.name)

    val fieldsByName: Map[Name, IArray[FieldTree]] =
      fields.groupBy(_.name)

    val resolvedParents: Option[ParentsResolver.Parents] =
      owner match {
        case x: InheritanceTree => Some(parentsResolver(scope, x))
        case _ => None
      }

    val parents: Map[TypeRef, ClassTree] =
      resolvedParents.fold(Map.empty[TypeRef, ClassTree])(_.transitiveParents)

    val (inheritedMethods, inheritedFields, _) =
      (ScalaJsClasses.jsObjectMembers ++ IArray.fromTraversable(parents).flatMap(_._2.members)).partitionCollect2(
        { case x: MethodTree => x },
        { case x: FieldTree  => x },
      )

    val inheritedFieldsByName: Map[Name, IArray[FieldTree]] =
      inheritedFields.groupBy(_.name)

    val inheritedMethodsByBase: Map[MethodBase, IArray[MethodTree]] =
      inheritedMethods.groupBy(erasure.base(scope))

    val inheritedMethodsByName: Map[Name, IArray[MethodTree]] =
      inheritedMethods.groupBy(_.name)

    /* Scala erases an inherited method as its declaring class declares it, not with our type arguments filled in.
     * `def m(p: P)` in `class A[P]` erases to `m(Object)` even where we inherit `A[Foo]`, so a
     * `def m(p: Q)` of our own clashes with it although the filled-in signatures differ. */
    val inheritedDeclaredBases: Set[MethodBase] = {
      /* diamond inheritance reaches the same declaring class along every path, and its erasure is
       * independent of the type arguments supplied at each use site, so visit each class once */
      def declared(
          acc:    (Set[QualifiedName], Set[MethodBase]),
          parent: ParentsResolver.Parent,
      ): (Set[QualifiedName], Set[MethodBase]) = {
        val (visited, bases) = acc
        val codePath         = parent.classTree.codePath
        if (visited(codePath)) acc
        else {
          val own = parent.foundIn
            .lookup(codePath)
            .collectFirst {
              case (cls: ClassTree, clsScope) =>
                cls.members.collect { case m: MethodTree => erasure.base(clsScope / cls)(m) }
            }
            .getOrElse(Empty)
          parent.parents.foldLeft((visited + codePath, own.foldLeft(bases)(_ + _)))(declared)
        }
      }
      resolvedParents.fold(Set.empty[MethodBase]) {
        _.directParents.foldLeft((Set.empty[QualifiedName], Set.empty[MethodBase]))(declared)._2
      }
    }

    val allMethods = inheritedMethodsByName ++ methodsByName
    val allFields  = inheritedFieldsByName ++ fieldsByName

    /* This is a bit of a hack.
      Ideally, we would run the transformation in such a manner that all parents were already done.
      That is not currently done, so here we approximate and say that if we inherit from a class with
       the same name (likely due to it being reexported) we probably already have retained the suffixed
       overloads we generate, so we won't do it again here
     */
    lazy val alreadySuffixed = inheritance.exists(_.name === owner.name)

    val newFields: IArray[FieldTree] = fields.flatMap { f =>
      allMethods.get(f.name) match {
        case Some(ms)
            if ms.exists(_.params.flatten.length === 0) || ScalaJsClasses.jsObjectMembers.exists(_.name === f.name) =>
          if (alreadySuffixed) Empty else IArray(f.withSuffix("F" + owner.name.value))
        case _ =>
          inheritedFieldsByName.get(f.name) match {
            case Some(conflicting: IArray[FieldTree]) =>
              /* but to retain a field with a different type, we rename it */
              val withSuffix = f.withSuffix(owner.name)

              if (f.tpe === TypeRef.Any || f.tpe === TypeRef.Nothing || (conflicting.exists(_.tpe === f.tpe)))
                /* there is no point in emitting duplicate fields */
                Empty
              else if (allFields.contains(withSuffix.name)) Empty
              else if (alreadySuffixed) Empty
              else IArray(withSuffix)

            case None =>
              IArray(f)
          }
      }
    }

    val newModules: IArray[ModuleTree] = modules.flatMap { m =>
      allMethods.get(m.name) match {
        case Some(ms)
            if ms.exists(_.params.flatten.length === 0) || ScalaJsClasses.jsObjectMembers.exists(_.name === m.name) =>
          Empty
        case _ => IArray(m)
      }
    }

    val newMethods: IArray[MethodTree] = methods.sorted(Sorter.TreeOrdering).flatMap {
      // remove setters if there is a corresponding var in a parent class
      case m
          if m.name.unescaped.endsWith("_=") &&
            inheritedFieldsByName
              .get(Name(m.name.unescaped.dropRight(2)))
              .exists(_.exists(f => !f.isReadOnly)) =>
        Empty
      case m if inheritedFieldsByName.contains(m.name) =>
        if (alreadySuffixed) Empty else IArray(m.withSuffix("M" + owner.name.value))
      case m =>
        val mBase = erasure.base(scope)(m)
        inheritedMethodsByBase.get(mBase) match {
          case Some(conflicting @ _)                          => Empty
          case None if inheritedDeclaredBases.contains(mBase) => Empty
          case _                                              => IArray(m)
        }
    }

    newFields ++ newMethods ++ newModules ++ other
  }
}
