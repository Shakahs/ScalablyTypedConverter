package typings.rxjs

import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod {
  
  @JSImport("rxjs", "Observable")
  @js.native
  open class Observable[T] ()
    extends typings.rxjs.internalObservableMod.Observable[T]
  
  type CompletionObserver[T] = typings.rxjs.internalTypesMod.CompletionObserver[T]
  
  type ErrorObserver[T] = typings.rxjs.internalTypesMod.ErrorObserver[T]
  
  type NextObserver[T] = typings.rxjs.internalTypesMod.NextObserver[T]
  
  type Observer[T] = typings.rxjs.internalTypesMod.Observer[T]
  
  type OperatorFunction[T, R] = typings.rxjs.internalTypesMod.OperatorFunction[T, R]
  
  type PartialObserver[T] = typings.rxjs.internalTypesMod.PartialObserver[T]
  
  type SchedulerAction[T] = typings.rxjs.internalTypesMod.SchedulerAction[T]
  
  type SchedulerLike = typings.rxjs.internalTypesMod.SchedulerLike
  
  type Subscribable[T] = typings.rxjs.internalTypesMod.Subscribable[T]
  
  type SubscriptionLike = typings.rxjs.internalTypesMod.SubscriptionLike
  
  type UnaryFunction[T, R] = typings.rxjs.internalTypesMod.UnaryFunction[T, R]
  
  type Unsubscribable = typings.rxjs.internalTypesMod.Unsubscribable
}
