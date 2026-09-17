package typings.agGridCommunity

import typings.agGridCommunity.mod.BaseCellDataType
import typings.agGridCommunity.mod.CommonFilterOptionKey
import typings.agGridCommunity.mod.DateFilterOptionKey
import typings.agGridCommunity.mod.ISimpleFilterModelPresetType
import typings.agGridCommunity.mod.ISimpleFilterModelType
import typings.agGridCommunity.mod.JoinOperator
import typings.agGridCommunity.mod.ScalarFilterOptionKey
import typings.agGridCommunity.mod.ShowValuesAsBuiltInType
import typings.agGridCommunity.mod.TextFilterOptionKey
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object agGridCommunityStrings {
  
  @js.native
  sealed trait AND
    extends StObject
       with JoinOperator
  inline def AND: AND = "AND".asInstanceOf[AND]
  
  @js.native
  sealed trait OR
    extends StObject
       with JoinOperator
  inline def OR: OR = "OR".asInstanceOf[OR]
  
  @js.native
  sealed trait blank
    extends StObject
       with CommonFilterOptionKey
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
       with TextFilterOptionKey
  inline def blank: blank = "blank".asInstanceOf[blank]
  
  @js.native
  sealed trait boolean
    extends StObject
       with BaseCellDataType
  inline def boolean: boolean = "boolean".asInstanceOf[boolean]
  
  @js.native
  sealed trait contains
    extends StObject
       with ISimpleFilterModelType
       with TextFilterOptionKey
  inline def contains: contains = "contains".asInstanceOf[contains]
  
  @js.native
  sealed trait empty
    extends StObject
       with CommonFilterOptionKey
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
       with TextFilterOptionKey
  inline def empty: empty = "empty".asInstanceOf[empty]
  
  @js.native
  sealed trait endsWith
    extends StObject
       with ISimpleFilterModelType
       with TextFilterOptionKey
  inline def endsWith: endsWith = "endsWith".asInstanceOf[endsWith]
  
  @js.native
  sealed trait equals
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
       with TextFilterOptionKey
  inline def equals: equals = "equals".asInstanceOf[equals]
  
  @js.native
  sealed trait greaterThan
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
  inline def greaterThan: greaterThan = "greaterThan".asInstanceOf[greaterThan]
  
  @js.native
  sealed trait greaterThanOrEqual
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
  inline def greaterThanOrEqual: greaterThanOrEqual = "greaterThanOrEqual".asInstanceOf[greaterThanOrEqual]
  
  @js.native
  sealed trait inRange
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
  inline def inRange: inRange = "inRange".asInstanceOf[inRange]
  
  @js.native
  sealed trait lastWeek
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelPresetType
       with ISimpleFilterModelType
  inline def lastWeek: lastWeek = "lastWeek".asInstanceOf[lastWeek]
  
  @js.native
  sealed trait lessThan
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
  inline def lessThan: lessThan = "lessThan".asInstanceOf[lessThan]
  
  @js.native
  sealed trait lessThanOrEqual
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
  inline def lessThanOrEqual: lessThanOrEqual = "lessThanOrEqual".asInstanceOf[lessThanOrEqual]
  
  @js.native
  sealed trait nextWeek
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelPresetType
       with ISimpleFilterModelType
  inline def nextWeek: nextWeek = "nextWeek".asInstanceOf[nextWeek]
  
  @js.native
  sealed trait notBlank
    extends StObject
       with CommonFilterOptionKey
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
       with TextFilterOptionKey
  inline def notBlank: notBlank = "notBlank".asInstanceOf[notBlank]
  
  @js.native
  sealed trait notContains
    extends StObject
       with ISimpleFilterModelType
       with TextFilterOptionKey
  inline def notContains: notContains = "notContains".asInstanceOf[notContains]
  
  @js.native
  sealed trait notEqual
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelType
       with ScalarFilterOptionKey
       with TextFilterOptionKey
  inline def notEqual: notEqual = "notEqual".asInstanceOf[notEqual]
  
  @js.native
  sealed trait number
    extends StObject
       with BaseCellDataType
  inline def number: number = "number".asInstanceOf[number]
  
  @js.native
  sealed trait percentOfRow
    extends StObject
       with ShowValuesAsBuiltInType
  inline def percentOfRow: percentOfRow = "percentOfRow".asInstanceOf[percentOfRow]
  
  @js.native
  sealed trait percentOfTotal
    extends StObject
       with ShowValuesAsBuiltInType
  inline def percentOfTotal: percentOfTotal = "percentOfTotal".asInstanceOf[percentOfTotal]
  
  @js.native
  sealed trait startsWith
    extends StObject
       with ISimpleFilterModelType
       with TextFilterOptionKey
  inline def startsWith: startsWith = "startsWith".asInstanceOf[startsWith]
  
  @js.native
  sealed trait text
    extends StObject
       with BaseCellDataType
  inline def text: text = "text".asInstanceOf[text]
  
  @js.native
  sealed trait thisWeek
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelPresetType
       with ISimpleFilterModelType
  inline def thisWeek: thisWeek = "thisWeek".asInstanceOf[thisWeek]
  
  @js.native
  sealed trait today
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelPresetType
       with ISimpleFilterModelType
  inline def today: today = "today".asInstanceOf[today]
  
  @js.native
  sealed trait tomorrow
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelPresetType
       with ISimpleFilterModelType
  inline def tomorrow: tomorrow = "tomorrow".asInstanceOf[tomorrow]
  
  @js.native
  sealed trait value
    extends StObject
       with ShowValuesAsBuiltInType
  inline def value: value = "value".asInstanceOf[value]
  
  @js.native
  sealed trait yesterday
    extends StObject
       with DateFilterOptionKey
       with ISimpleFilterModelPresetType
       with ISimpleFilterModelType
  inline def yesterday: yesterday = "yesterday".asInstanceOf[yesterday]
}
