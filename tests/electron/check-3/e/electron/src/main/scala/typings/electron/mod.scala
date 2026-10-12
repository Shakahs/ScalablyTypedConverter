package typings.electron

import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod {
  
  @JSImport("electron", "EventEmitter")
  @js.native
  open class EventEmitter ()
    extends StObject
       with typings.electron.Electron.EventEmitter {
    
    /* CompleteClass */
    override def addListener(event: String, listener: js.Function): this.type = js.native
  }
  
  @JSImport("electron", "app")
  @js.native
  val app: typings.electron.Electron.App = js.native
  
  type AllElectron = typings.electron.Electron.AllElectron
  
  type App = typings.electron.Electron.App
  
  type CommonInterface = typings.electron.Electron.CommonInterface
  
  type Event = typings.electron.Electron.Event
  
  type MainInterface = typings.electron.Electron.MainInterface
}
