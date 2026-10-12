package typings.conditionalTypeCycle

import typings.conditionalTypeCycle.conditionalTypeCycleBooleans.`true`
import typings.std.NonNullable
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod {
  
  @JSImport("conditional-type-cycle", JSImport.Namespace)
  @js.native
  val ^ : js.Any = js.native
  
  inline def select[TSelectObject](selectObject: TSelectObject): ResultTypeFromSelect[TSelectObject] = ^.asInstanceOf[js.Dynamic].applyDynamic("select")(selectObject.asInstanceOf[js.Any]).asInstanceOf[ResultTypeFromSelect[TSelectObject]]
  
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * You'll have to cast your way around this structure, unfortunately.
    * TS definition: {{{
    T extends object ? {[ K in keyof T ]:? never} : never
    }}}
    */
  @js.native
  trait EmptySelectedRef[T] extends StObject
  
  type ExtractDirectSelectRef[T] = NonNullable[ExtractRef[T]] | EmptySelectedRef[NonNullable[ExtractRef[T]]]
  
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * You'll have to cast your way around this structure, unfortunately.
    * TS definition: {{{
    T extends conditional-type-cycle.conditional-type-cycle.Ref<infer U> ? U : conditional-type-cycle.conditional-type-cycle.Prettify<conditional-type-cycle.conditional-type-cycle.ResultTypeFromSelect<T>>
    }}}
    */
  @js.native
  trait ExtractRef[T] extends StObject
  
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * This RHS of the type alias is guess work. You should cast if it's not correct in your case.
    * TS definition: {{{
    T extends conditional-type-cycle.conditional-type-cycle.Ref<infer _U> ? true : false
    }}}
    */
  type IsTrueRef[T] = `true`
  
  type Prettify[T] = /* import warning: importer.ImportType#apply Failed type conversion: {[ K in keyof T ]: T[K]} */ js.Any
  
  trait Ref[T] extends StObject {
    
    val __ref: T
  }
  object Ref {
    
    inline def apply[T](__ref: T): Ref[T] = {
      val __obj = js.Dynamic.literal(__ref = __ref.asInstanceOf[js.Any])
      __obj.asInstanceOf[Ref[T]]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: Ref[?], T] (val x: Self & Ref[T]) extends AnyVal {
      
      inline def set__ref(value: T): Self = StObject.set(x, "__ref", value.asInstanceOf[js.Any])
    }
  }
  
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * You'll have to cast your way around this structure, unfortunately.
    * TS definition: {{{
    conditional-type-cycle.conditional-type-cycle.IsTrueRef<TSelectObject> extends true ? conditional-type-cycle.conditional-type-cycle.ExtractDirectSelectRef<TSelectObject> : {[ K in keyof TSelectObject ]: TSelectObject[K] extends conditional-type-cycle.conditional-type-cycle.Ref<infer T>? T : conditional-type-cycle.conditional-type-cycle.ResultTypeFromSelect<TSelectObject[K]>}
    }}}
    */
  @js.native
  trait ResultTypeFromSelect[TSelectObject] extends StObject
}
