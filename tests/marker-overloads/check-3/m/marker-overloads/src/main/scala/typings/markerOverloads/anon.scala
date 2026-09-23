package typings.markerOverloads

import typings.markerOverloads.mod.Signal
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object anon {
  
  /* Inlined marker-overloads.marker-overloads.QueryContext<string, never> */
  trait QueryContextstringnever extends StObject {
    
    var queryKey: String
    
    var signal: Signal
  }
  object QueryContextstringnever {
    
    inline def apply(queryKey: String, signal: Signal): QueryContextstringnever = {
      val __obj = js.Dynamic.literal(queryKey = queryKey.asInstanceOf[js.Any], signal = signal.asInstanceOf[js.Any])
      __obj.asInstanceOf[QueryContextstringnever]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: QueryContextstringnever] (val x: Self) extends AnyVal {
      
      inline def setQueryKey(value: String): Self = StObject.set(x, "queryKey", value.asInstanceOf[js.Any])
      
      inline def setSignal(value: Signal): Self = StObject.set(x, "signal", value.asInstanceOf[js.Any])
    }
  }
  
  /* Inlined marker-overloads.marker-overloads.QueryContext<string, number> */
  trait QueryContextstringnumber extends StObject {
    
    var pageParam: Double
    
    var queryKey: String
    
    var signal: Signal
  }
  object QueryContextstringnumber {
    
    inline def apply(pageParam: Double, queryKey: String, signal: Signal): QueryContextstringnumber = {
      val __obj = js.Dynamic.literal(pageParam = pageParam.asInstanceOf[js.Any], queryKey = queryKey.asInstanceOf[js.Any], signal = signal.asInstanceOf[js.Any])
      __obj.asInstanceOf[QueryContextstringnumber]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: QueryContextstringnumber] (val x: Self) extends AnyVal {
      
      inline def setPageParam(value: Double): Self = StObject.set(x, "pageParam", value.asInstanceOf[js.Any])
      
      inline def setQueryKey(value: String): Self = StObject.set(x, "queryKey", value.asInstanceOf[js.Any])
      
      inline def setSignal(value: Signal): Self = StObject.set(x, "signal", value.asInstanceOf[js.Any])
    }
  }
  
  trait Schema extends StObject {
    
    var schema: js.UndefOr[scala.Nothing] = js.undefined
  }
}
