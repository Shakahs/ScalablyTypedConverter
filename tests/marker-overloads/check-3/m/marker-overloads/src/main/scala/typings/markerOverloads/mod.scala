package typings.markerOverloads

import typings.markerOverloads.anon.QueryContextstringnever
import typings.markerOverloads.anon.QueryContextstringnumber
import typings.markerOverloads.anon.Schema
import typings.markerOverloads.markerOverloadsBooleans.`false`
import typings.markerOverloads.markerOverloadsBooleans.`true`
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod {
  
  @JSImport("marker-overloads", JSImport.Namespace)
  @js.native
  val ^ : js.Any = js.native
  
  inline def createCollection[T /* <: js.Object */](
    options: (CollectionConfig[T] & Schema & NonSingleResult) | (CollectionConfig[T] & Schema & SingleResult)
  ): Collection[T] & NonSingleResult = ^.asInstanceOf[js.Dynamic].applyDynamic("createCollection")(options.asInstanceOf[js.Any]).asInstanceOf[Collection[T] & NonSingleResult]
  
  inline def pagedQuery(fn: js.Function1[/* context */ QueryContextstringnumber, Unit]): Unit = ^.asInstanceOf[js.Dynamic].applyDynamic("pagedQuery")(fn.asInstanceOf[js.Any]).asInstanceOf[Unit]
  
  inline def query(fn: js.Function1[/* context */ QueryContextstringnever, Unit]): Unit = ^.asInstanceOf[js.Dynamic].applyDynamic("query")(fn.asInstanceOf[js.Any]).asInstanceOf[Unit]
  
  inline def refetching(interval: Double): Unit = ^.asInstanceOf[js.Dynamic].applyDynamic("refetching")(interval.asInstanceOf[js.Any]).asInstanceOf[Unit]
  
  inline def refetching_false(interval: `false`): Unit = ^.asInstanceOf[js.Dynamic].applyDynamic("refetching")(interval.asInstanceOf[js.Any]).asInstanceOf[Unit]
  
  inline def retrying(retry: Boolean): Unit = ^.asInstanceOf[js.Dynamic].applyDynamic("retrying")(retry.asInstanceOf[js.Any]).asInstanceOf[Unit]
  inline def retrying(retry: Double): Unit = ^.asInstanceOf[js.Dynamic].applyDynamic("retrying")(retry.asInstanceOf[js.Any]).asInstanceOf[Unit]
  
  trait Collection[T] extends StObject {
    
    def get(key: String): js.UndefOr[T]
  }
  object Collection {
    
    inline def apply[T](get: String => js.UndefOr[T]): Collection[T] = {
      val __obj = js.Dynamic.literal(get = js.Any.fromFunction1(get))
      __obj.asInstanceOf[Collection[T]]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: Collection[?], T] (val x: Self & Collection[T]) extends AnyVal {
      
      inline def setGet(value: String => js.UndefOr[T]): Self = StObject.set(x, "get", js.Any.fromFunction1(value))
    }
  }
  
  trait CollectionConfig[T] extends StObject {
    
    def getKey(item: T): String
  }
  object CollectionConfig {
    
    inline def apply[T](getKey: T => String): CollectionConfig[T] = {
      val __obj = js.Dynamic.literal(getKey = js.Any.fromFunction1(getKey))
      __obj.asInstanceOf[CollectionConfig[T]]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: CollectionConfig[?], T] (val x: Self & CollectionConfig[T]) extends AnyVal {
      
      inline def setGetKey(value: T => String): Self = StObject.set(x, "getKey", js.Any.fromFunction1(value))
    }
  }
  
  trait NonSingleResult extends StObject {
    
    var singleResult: js.UndefOr[scala.Nothing] = js.undefined
  }
  
  trait ObserverOptions extends StObject {
    
    var refetchInterval: Double | `false`
    
    var retry: Boolean | Double
  }
  object ObserverOptions {
    
    inline def apply(refetchInterval: Double | `false`, retry: Boolean | Double): ObserverOptions = {
      val __obj = js.Dynamic.literal(refetchInterval = refetchInterval.asInstanceOf[js.Any], retry = retry.asInstanceOf[js.Any])
      __obj.asInstanceOf[ObserverOptions]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: ObserverOptions] (val x: Self) extends AnyVal {
      
      inline def setRefetchInterval(value: Double | `false`): Self = StObject.set(x, "refetchInterval", value.asInstanceOf[js.Any])
      
      inline def setRetry(value: Boolean | Double): Self = StObject.set(x, "retry", value.asInstanceOf[js.Any])
    }
  }
  
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * You'll have to cast your way around this structure, unfortunately.
    * TS definition: {{{
    [TPage] extends [never] ? {  queryKey :TKey,   signal :marker-overloads.marker-overloads.Signal} : {  queryKey :TKey,   signal :marker-overloads.marker-overloads.Signal,   pageParam :TPage}
    }}}
    */
  @js.native
  trait QueryContext[TKey, TPage] extends StObject
  
  trait Signal extends StObject {
    
    var aborted: Boolean
  }
  object Signal {
    
    inline def apply(aborted: Boolean): Signal = {
      val __obj = js.Dynamic.literal(aborted = aborted.asInstanceOf[js.Any])
      __obj.asInstanceOf[Signal]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: Signal] (val x: Self) extends AnyVal {
      
      inline def setAborted(value: Boolean): Self = StObject.set(x, "aborted", value.asInstanceOf[js.Any])
    }
  }
  
  trait SingleResult extends StObject {
    
    var singleResult: `true`
  }
  object SingleResult {
    
    inline def apply(): SingleResult = {
      val __obj = js.Dynamic.literal(singleResult = true)
      __obj.asInstanceOf[SingleResult]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: SingleResult] (val x: Self) extends AnyVal {
      
      inline def setSingleResult(value: `true`): Self = StObject.set(x, "singleResult", value.asInstanceOf[js.Any])
    }
  }
}
