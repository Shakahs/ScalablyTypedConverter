package typings.vue

import org.scalablytyped.runtime.Shortcut
import typings.vue.typesVueMod.Vue
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod extends Shortcut {
  
  @JSImport("vue", JSImport.Default)
  @js.native
  val default: typings.vue.typesVueMod.VueConstructor[Vue] = js.native
  type default = Vue
  
  type AsyncComponent[Data, Methods, Computed, Props] = typings.vue.typesOptionsMod.AsyncComponent[Data, Methods, Computed, Props]
  
  type Component[Data, Methods, Computed, Props] = typings.vue.typesOptionsMod.Component[Data, Methods, Computed, Props]
  
  type ComponentOptions[V /* <: Vue */, Data, Methods, Computed, PropsDef] = typings.vue.typesOptionsMod.ComponentOptions[V, Data, Methods, Computed, PropsDef]
  
  type ComputedOptions[T] = typings.vue.typesOptionsMod.ComputedOptions[T]
  
  type CreateElement = typings.vue.typesVueMod.CreateElement
  
  type DirectiveFunction = typings.vue.typesOptionsMod.DirectiveFunction
  
  type DirectiveOptions = typings.vue.typesOptionsMod.DirectiveOptions
  
  type FunctionalComponentOptions[Props, PropDefs] = typings.vue.typesOptionsMod.FunctionalComponentOptions[Props, PropDefs]
  
  type PluginFunction[T] = typings.vue.typesPluginMod.PluginFunction[T]
  
  type PluginObject[T] = typings.vue.typesPluginMod.PluginObject[T]
  
  type PropOptions[T] = typings.vue.typesOptionsMod.PropOptions[T]
  
  type RenderContext[Props] = typings.vue.typesOptionsMod.RenderContext[Props]
  
  type VNode = typings.vue.typesVnodeMod.VNode
  
  type VNodeChildren = typings.vue.typesVnodeMod.VNodeChildren
  
  type VNodeChildrenArrayContents = typings.vue.typesVnodeMod.VNodeChildrenArrayContents
  
  type VNodeComponentOptions = typings.vue.typesVnodeMod.VNodeComponentOptions
  
  type VNodeData = typings.vue.typesVnodeMod.VNodeData
  
  type VNodeDirective = typings.vue.typesVnodeMod.VNodeDirective
  
  type VueConstructor[V /* <: Vue */] = typings.vue.typesVueMod.VueConstructor[V]
  
  type WatchHandler[T] = typings.vue.typesOptionsMod.WatchHandler[T]
  
  type WatchOptions = typings.vue.typesOptionsMod.WatchOptions
  
  type WatchOptionsWithHandler[T] = typings.vue.typesOptionsMod.WatchOptionsWithHandler[T]
  
  type _To = typings.vue.typesVueMod.VueConstructor[Vue]
  
  /* This means you don't have to write `default`, but can instead just say `mod.foo` */
  override def _to: typings.vue.typesVueMod.VueConstructor[Vue] = default
}
