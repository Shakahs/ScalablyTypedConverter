package org.scalablytyped.converter.internal
package scalajs
package flavours

import org.scalablytyped.converter.internal.scalajs.ExprTree._

/**
  * The builder every generated component returns: the three arguments of `React.createElement`, each with its own
  * type, which `build` creates the element from.
  *
  * The fields are prefixed so they don't collide with the setter a component generates for each of its props, which
  * are named after the props (`component`, `props` and `children` among them).
  */
class JapgollyGenStBuildingComponent(val outputPkg: Name, val scalaVersion: Versions.Scala) {

  val StBuildingComponent = Name("StBuildingComponent")
  val builderCp           = QualifiedName(IArray(outputPkg, StBuildingComponent))
  val R                   = TypeParamTree(Name("R"), Empty, Some(TypeRef.JsObject), NoComments, ignoreBound = false)
  val builderTparams      = IArray(R)
  val builderRef          = TypeRef(builderCp, TypeParamTree.asTypeArgs(builderTparams), NoComments)
  val modParam            = ParamTree(Name("m"), false, false, TypeRef(JapgollyNames.vdom.TagMod), NotImplemented, NoComments)
  val propsParam          = ParamTree(Name("m"), false, false, TypeRef.JsObject, NotImplemented, NoComments)

  val ElementType = TypeRef(JapgollyNames.rawReact.ElementType)
  val ReactNode   = TypeRef(JapgollyNames.rawReact.Node)
  val Children    = TypeRef(QualifiedName.JsArray, IArray(ReactNode), NoComments)

  private def field(name: Name, tpe: TypeRef): MethodTree =
    MethodTree(
      Empty,
      ProtectionLevel.Public,
      name,
      Empty,
      Empty,
      NotImplemented,
      tpe,
      isOverride = false,
      comments   = NoComments,
      codePath   = builderCp + name,
      isImplicit = false,
    )

  //  def stElementType: React.ElementType
  val elementType: MethodTree = field(Name("stElementType"), ElementType)
  //  def stProps: js.Object
  val props: MethodTree = field(Name("stProps"), TypeRef.JsObject)
  //  def stChildren: js.Array[React.Node]
  val children: MethodTree = field(Name("stChildren"), Children)

  /** The parameters a builder is constructed with, one for each field; it starts without children. */
  val ctorParams: IArray[ParamTree] =
    IArray(
      ParamTree(elementType.name, isImplicit = false, isVal = true, ElementType, NotImplemented, NoComments),
      ParamTree(props.name, isImplicit = false, isVal = true, TypeRef.JsObject, NotImplemented, NoComments),
      ParamTree(
        children.name,
        isImplicit = false,
        isVal      = true,
        Children,
        Call(Ref(QualifiedName.JsArray), IArray(Empty)),
        NoComments,
      ),
    )

  //  @inline final def set(key: String, value: js.Any): this.type = {
  //    stProps.asInstanceOf[js.Dynamic].updateDynamic(key)(value)
  //    this
  //  }
  val set: MethodTree = {
    val keyParam =
      ParamTree(Name("key"), isImplicit = false, isVal = false, TypeRef.String, NotImplemented, NoComments)
    val valueParam =
      ParamTree(Name("value"), isImplicit = false, isVal = false, TypeRef.JsAny, NotImplemented, NoComments)

    val impl = Block(
      Call(
        Select(AsInstanceOf(Ref(props.name), TypeRef.JsDynamic), Name("updateDynamic")),
        IArray(IArray(Ref(keyParam.name)), IArray(Ref(valueParam.name))),
      ),
      Ref(QualifiedName.THIS),
    )
    val name: Name = Name("set")

    MethodTree(
      IArray(Annotation.Inline),
      ProtectionLevel.Public,
      name,
      Empty,
      IArray(IArray(keyParam, valueParam)),
      impl,
      TypeRef(QualifiedName.THIS),
      isOverride = false,
      NoComments,
      builderCp + name,
      isImplicit = false,
    )
  }

  val Trait: ClassTree = {
    val unsafeSpread = {
      val param = ParamTree(Name("obj"), false, false, TypeRef.JsObject, NotImplemented, NoComments)
      val name  = Name("unsafeSpread")
      val assign = Call(
        Ref(QualifiedName.JsObject + Name("assign")),
        IArray(IArray(Ref(props.name), Ref(param.name))),
      )

      MethodTree(
        IArray(Annotation.Inline),
        ProtectionLevel.Public,
        name,
        Empty,
        IArray(IArray(param)),
        Block(assign, Ref(QualifiedName.THIS)),
        TypeRef(QualifiedName.THIS),
        false,
        NoComments,
        builderCp + name,
        false,
      )
    }

    val build = {
      val name = Name("build")

      MethodTree(
        IArray(Annotation.Inline),
        ProtectionLevel.Public,
        name,
        Empty,
        Empty,
        Call(Ref(Object.make.codePath), IArray(IArray(Ref(QualifiedName.THIS)))),
        Object.make.resultType,
        false,
        Comments(
          Comment(
            "/* You typically shouldnt call this yourself, but it can be useful if you're for instance mapping a sequence and you need types to infer properly */\n",
          ),
        ),
        builderCp + name,
        false,
      )
    }

    //    @scala.inline
//    def applyTagMod(t: TagMod): Unit =
//      if (t.isInstanceOf[TagMod.Composite]) {
//        val tt = t.asInstanceOf[TagMod.Composite]
//        tt.mods.foreach((m: TagMod) => applyTagMod(m))
//      } else if (t.isInstanceOf[VdomNode]) {
//        val tt = t.asInstanceOf[VdomNode]
//        stChildren.push(tt.rawNode)
//      } else {
//        val tt = t.toJs
//        tt.addClassNameToProps()
//        tt.addKeyToProps()
//        tt.addStyleToProps()
//        tt.nonEmptyChildren.foreach((children: js.Array[Node]) => stChildren.push(children.toSeq*))
//        tt.nonEmptyProps.foreach((m: js.Object) => unsafeSpread(m))
//      }
    val applyTagMod = {
      val TagMod          = TypeRef(JapgollyNames.vdom.TagMod)
      val VdomNode        = TypeRef(JapgollyNames.vdom.VdomNode)
      val TagModComposite = TypeRef(JapgollyNames.vdom.TagMod + Name("Composite"))

      val tParam = ParamTree(Name("t"), false, false, TagMod, NotImplemented, NoComments)
      val name   = Name("applyTagMod")
      val impl = {
        val ttName = Name("tt")
        val fallback = {
          val childrenName = Name("children")
          /* each child its own argument, as scalajs-react passes a tag's children */
          val childrenLambda = Lambda(
            IArray(ParamTree(childrenName, false, false, Children, NotImplemented, NoComments)),
            Call(Select(Ref(children.name), Name("push")), IArray(IArray(`:_*`(Ref(childrenName).select("toSeq"))))),
          )
          Block(
            Val(ttName, Select(Ref(tParam.name), Name("toJs"))),
            Call(Ref(QualifiedName(IArray(ttName, Name("addClassNameToProps")))), IArray(Empty)),
            Call(Ref(QualifiedName(IArray(ttName, Name("addKeyToProps")))), IArray(Empty)),
            Call(Ref(QualifiedName(IArray(ttName, Name("addStyleToProps")))), IArray(Empty)),
            Call(
              Ref(QualifiedName(IArray(ttName, Name("nonEmptyChildren"), Name("foreach")))),
              IArray(IArray(childrenLambda)),
            ),
            Call(
              Ref(QualifiedName(IArray(ttName, Name("nonEmptyProps"), Name("foreach")))),
              IArray(
                IArray(Lambda(IArray(propsParam), Call(Ref(unsafeSpread.name), IArray(IArray(Ref(propsParam.name)))))),
              ),
            ),
          )
        }

        If(
          IsInstanceOf(Ref(tParam.name), TagModComposite),
          Block(
            Val(ttName, AsInstanceOf(Ref(tParam.name), TagModComposite)),
            Call(
              Ref(QualifiedName(IArray(ttName, Name("mods"), Name("foreach")))),
              IArray(IArray(Lambda(IArray(modParam), Call(Ref(name), IArray(IArray(Ref(modParam.name))))))),
            ),
          ),
          Some(
            If(
              IsInstanceOf(Ref(tParam.name), VdomNode),
              Block(
                Val(ttName, AsInstanceOf(Ref(tParam.name), VdomNode)),
                Call(
                  Select(Ref(children.name), Name("push")),
                  IArray(IArray(Select(Ref(ttName), Name("rawNode")))),
                ),
              ),
              Some(fallback),
            ),
          ),
        )
      }

      MethodTree(
        Empty,
        ProtectionLevel.Public,
        name,
        Empty,
        IArray(IArray(tParam)),
        impl,
        TypeRef.Unit,
        false,
        NoComments,
        builderCp + Name.APPLY + name,
        false,
      )
    }

//    @scala.inline
//    def apply(mods: TagMod*): this.type = {
//      mods.foreach((m: TagMod) => applyTagMod(m))
//      this
//    }
    val `apply` = {
      val modsParam = ParamTree(
        name       = Name("mods"),
        isImplicit = false,
        isVal      = false,
        tpe        = TypeRef.Repeated(TypeRef(JapgollyNames.vdom.TagMod), NoComments),
        default    = NotImplemented,
        comments   = NoComments,
      )
      val impl =
        Block(
          Call(
            Select(Ref(modsParam.name), Name("foreach")),
            IArray(IArray(Lambda(IArray(modParam), Call(Ref(applyTagMod.name), IArray(IArray(Ref(modParam.name))))))),
          ),
          Ref(Name.THIS),
        )

      MethodTree(
        annotations = IArray(Annotation.Inline),
        level       = ProtectionLevel.Public,
        name        = Name.APPLY,
        tparams     = Empty,
        params      = IArray(IArray(modsParam)),
        impl,
        resultType = TypeRef(QualifiedName.THIS),
        isOverride = false,
        comments   = NoComments,
        codePath   = builderCp + Name.APPLY,
        isImplicit = false,
      )
    }

//    @scala.inline
//    def children(children: japgolly.scalajs.react.PropsChildren): this.type = {
//      stChildren.push(children.raw)
//      this
//    }
    val applyChildren = {
      val childrenParam = ParamTree(
        name       = Name("children"),
        isImplicit = false,
        isVal      = false,
        tpe        = TypeRef(JapgollyNames.PropsChildren),
        default    = NotImplemented,
        comments   = NoComments,
      )
      val impl =
        Block(
          Call(
            Ref(children.name).select("push"),
            IArray(IArray(Ref(childrenParam.name).select("raw"))),
          ),
          Ref(Name.THIS),
        )

      val name = Name("children")
      MethodTree(
        annotations = IArray(Annotation.Inline),
        level       = ProtectionLevel.Public,
        name        = name,
        tparams     = Empty,
        params      = IArray(IArray(childrenParam)),
        impl,
        resultType = TypeRef(QualifiedName.THIS),
        isOverride = false,
        comments   = NoComments,
        codePath   = builderCp + name,
        isImplicit = false,
      )
    }

    //  @inline final def withKey(key: japgolly.scalajs.react.Key): this.type = set("key", key)
    val withKey = {
      val param = ParamTree(
        Name("key"),
        isImplicit = false,
        isVal      = false,
        TypeRef(JapgollyNames.Key),
        NotImplemented,
        NoComments,
      )
      val name = Name("withKey")
      MethodTree(
        annotations = IArray(Annotation.Inline),
        level       = ProtectionLevel.Public,
        name        = name,
        tparams     = Empty,
        params      = IArray(IArray(param)),
        impl = Call(
          Ref(set.name),
          IArray(IArray(StringLit(param.name.unescaped), AsInstanceOf(Ref(param.name), TypeRef.JsAny))),
        ),
        resultType = TypeRef(QualifiedName.THIS),
        isOverride = false,
        comments   = NoComments,
        codePath   = builderCp + name,
        isImplicit = false,
      )
    }

    //
    //  @inline final def withRef(ref: js.Function1[R, Unit]): this.type = set("ref", ref)
    val withRef1 = {
      val tpe   = TypeRef.JsFunction(None, IArray(TypeRef(R.name)), TypeRef.Unit, NoComments)
      val param = ParamTree(Name("ref"), isImplicit = false, isVal = false, tpe, NotImplemented, NoComments)
      val name  = Name("withRef")
      MethodTree(
        annotations = IArray(Annotation.Inline),
        level       = ProtectionLevel.Public,
        name        = name,
        tparams     = Empty,
        params      = IArray(IArray(param)),
        impl        = Call(Ref(set.name), IArray(IArray(StringLit(param.name.unescaped), Ref(param.name)))),
        resultType  = TypeRef(QualifiedName.THIS),
        isOverride  = false,
        comments    = NoComments,
        codePath    = builderCp + name,
        isImplicit  = false,
      )
    }
    //
    //  @inline final def withRef(ref: japgolly.scalajs.react.Ref.Simple[R]): this.type = set("ref", ref.rawSetFn)
    val withRef2 = {
      val tpe   = TypeRef(JapgollyNames.RefSimple, IArray(TypeRef(R.name)), NoComments)
      val param = ParamTree(Name("ref"), isImplicit = false, isVal = false, tpe, NotImplemented, NoComments)
      val name  = Name("withRef")
      MethodTree(
        annotations = IArray(Annotation.Inline),
        level       = ProtectionLevel.Public,
        name        = name,
        tparams     = Empty,
        params      = IArray(IArray(param)),
        impl = Call(
          Ref(set.name),
          IArray(IArray(StringLit(param.name.unescaped), Select(Ref(param.name), Name("rawSetFn")))),
        ),
        resultType = TypeRef(QualifiedName.THIS),
        isOverride = false,
        comments   = NoComments,
        codePath   = builderCp + name,
        isImplicit = false,
      )
    }

    ClassTree(
      isImplicit  = false,
      annotations = Empty,
      level       = ProtectionLevel.Public,
      name        = StBuildingComponent,
      tparams     = builderTparams,
      parents     = Empty,
      ctors       = Empty,
      members = IArray(
        elementType,
        props,
        children,
        set,
        unsafeSpread,
        build,
        applyTagMod,
        apply,
        applyChildren,
        withKey,
        withRef1,
        withRef2,
      ),
      classType = ClassType.Trait,
      isSealed  = false,
      comments  = NoComments,
      codePath  = builderCp,
    )
  }

  lazy val Default: ClassTree = {
    val ctor = CtorTree(ProtectionLevel.Public, ctorParams, NoComments)

    val name = Name("Default")
    ClassTree(
      isImplicit  = false,
      annotations = Empty,
      level       = ProtectionLevel.Public,
      name        = name,
      tparams     = builderTparams,
      parents     = IArray(builderRef),
      ctors       = IArray(ctor),
      members     = Empty,
      classType   = ClassType.Class,
      isSealed    = false,
      comments    = NoComments,
      codePath    = builderCp + name,
    )
  }

  object Object {
    //    @js.native
    //    @JSImport("react", JSImport.Namespace, "React")
    //    object ReactRaw extends js.Object {
    //      val createElement: js.Dynamic = js.native // used for WithAttrs
    //    }
    val ReactRaw = {
      val name          = Name("ReactRaw")
      val ReactRawCp    = builderCp + name
      val createElement = Name("createElement")
      //  def createElement(elementType: React.ElementType, props: js.Object, children: React.Node*): React.Element
      val createElementMethod = MethodTree(
        annotations = Empty,
        level       = ProtectionLevel.Public,
        name        = createElement,
        tparams     = Empty,
        params = IArray(
          IArray(
            ParamTree(Name("elementType"), false, false, ElementType, NotImplemented, NoComments),
            ParamTree(Name("props"), false, false, TypeRef.JsObject, NotImplemented, NoComments),
            ParamTree(Name("children"), false, false, TypeRef.Repeated(ReactNode, NoComments), NotImplemented, NoComments),
          ),
        ),
        impl       = ExprTree.native,
        resultType = TypeRef(JapgollyNames.rawReact.Element),
        isOverride = false,
        comments   = NoComments,
        codePath   = ReactRawCp + createElement,
        isImplicit = false,
      )
      ModuleTree(
        IArray(
          Annotation.JsImport("react", Imported.Namespace, Some(Annotation.JsGlobal(QualifiedName("React")))),
          Annotation.JsNative,
        ), // todo global
        ProtectionLevel.Public,
        name,
        IArray(TypeRef.JsObject),
        IArray(createElementMethod),
        Minimization.KeepMarker + Marker.ManglerLeaveAlone,
        ReactRawCp,
        isOverride = false,
      )
    }
//    @inline implicit def make[R <: js.Object](comp: StBuildingComponent[R]): japgolly.scalajs.react.vdom.VdomElement =
//      japgolly.scalajs.react.vdom.VdomElement(ReactRaw.createElement(comp.stElementType, comp.stProps, comp.stChildren.toSeq*))
//
// createElement copies the props and children it is given, so the builder can be built again.
    val make: MethodTree = {
      val compParam = ParamTree(Name("comp"), false, false, builderRef, NotImplemented, NoComments)
      val name      = Name("make")
      val comp      = Ref(compParam.name)

      val impl = Call(
        Ref(JapgollyNames.vdom.ReactElement),
        IArray(
          IArray(
            Call(
              Ref(ReactRaw.name).select("createElement"),
              IArray(
                IArray(
                  Select(comp, elementType.name),
                  Select(comp, props.name),
                  `:_*`(Select(comp, children.name).select("toSeq")),
                ),
              ),
            ),
          ),
        ),
      )

      MethodTree(
        IArray(Annotation.Inline),
        ProtectionLevel.Public,
        name,
        builderTparams,
        IArray(IArray(compParam)),
        impl,
        TypeRef(JapgollyNames.vdom.ReactElement),
        isOverride = false,
        NoComments,
        builderCp + name,
        isImplicit = true,
      )
    }

    val tree = ModuleTree(
      Empty,
      ProtectionLevel.Public,
      StBuildingComponent,
      Empty,
      IArray(ReactRaw, make, Default),
      Minimization.KeepMarker + Marker.ManglerLeaveAlone,
      builderCp,
      isOverride = false,
    )
  }
}
