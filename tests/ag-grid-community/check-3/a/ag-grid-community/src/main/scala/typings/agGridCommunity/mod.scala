package typings.agGridCommunity

import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod {
  
  @JSImport("ag-grid-community", JSImport.Namespace)
  @js.native
  val ^ : js.Any = js.native
  
  inline def isCombinedFilterModel[M /* <: ISimpleFilterModel */](model: M): /* is ag-grid-community.ag-grid-community.ICombinedSimpleModel<M> */ Boolean = ^.asInstanceOf[js.Dynamic].applyDynamic("isCombinedFilterModel")(model.asInstanceOf[js.Any]).asInstanceOf[/* is ag-grid-community.ag-grid-community.ICombinedSimpleModel<M> */ Boolean]
  inline def isCombinedFilterModel[M /* <: ISimpleFilterModel */](model: ICombinedSimpleModel[M]): /* is ag-grid-community.ag-grid-community.ICombinedSimpleModel<M> */ Boolean = ^.asInstanceOf[js.Dynamic].applyDynamic("isCombinedFilterModel")(model.asInstanceOf[js.Any]).asInstanceOf[/* is ag-grid-community.ag-grid-community.ICombinedSimpleModel<M> */ Boolean]
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.text
    - typings.agGridCommunity.agGridCommunityStrings.number
    - typings.agGridCommunity.agGridCommunityStrings.boolean
  */
  trait BaseCellDataType extends StObject
  object BaseCellDataType {
    
    inline def boolean: typings.agGridCommunity.agGridCommunityStrings.boolean = "boolean".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.boolean]
    
    inline def number: typings.agGridCommunity.agGridCommunityStrings.number = "number".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.number]
    
    inline def text: typings.agGridCommunity.agGridCommunityStrings.text = "text".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.text]
  }
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.empty
    - typings.agGridCommunity.agGridCommunityStrings.blank
    - typings.agGridCommunity.agGridCommunityStrings.notBlank
  */
  trait CommonFilterOptionKey extends StObject
  object CommonFilterOptionKey {
    
    inline def blank: typings.agGridCommunity.agGridCommunityStrings.blank = "blank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.blank]
    
    inline def empty: typings.agGridCommunity.agGridCommunityStrings.empty = "empty".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.empty]
    
    inline def notBlank: typings.agGridCommunity.agGridCommunityStrings.notBlank = "notBlank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notBlank]
  }
  
  type CustomFilterOptionKey = String
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.equals
    - typings.agGridCommunity.agGridCommunityStrings.notEqual
    - typings.agGridCommunity.agGridCommunityStrings.lessThan
    - typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual
    - typings.agGridCommunity.agGridCommunityStrings.greaterThan
    - typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual
    - typings.agGridCommunity.agGridCommunityStrings.inRange
    - typings.agGridCommunity.agGridCommunityStrings.empty
    - typings.agGridCommunity.agGridCommunityStrings.blank
    - typings.agGridCommunity.agGridCommunityStrings.notBlank
    - typings.agGridCommunity.agGridCommunityStrings.today
    - typings.agGridCommunity.agGridCommunityStrings.yesterday
    - typings.agGridCommunity.agGridCommunityStrings.tomorrow
    - typings.agGridCommunity.agGridCommunityStrings.thisWeek
    - typings.agGridCommunity.agGridCommunityStrings.lastWeek
    - typings.agGridCommunity.agGridCommunityStrings.nextWeek
  */
  trait DateFilterOptionKey extends StObject
  object DateFilterOptionKey {
    
    inline def blank: typings.agGridCommunity.agGridCommunityStrings.blank = "blank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.blank]
    
    inline def empty: typings.agGridCommunity.agGridCommunityStrings.empty = "empty".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.empty]
    
    inline def equals: typings.agGridCommunity.agGridCommunityStrings.equals = "equals".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.equals]
    
    inline def greaterThan: typings.agGridCommunity.agGridCommunityStrings.greaterThan = "greaterThan".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.greaterThan]
    
    inline def greaterThanOrEqual: typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual = "greaterThanOrEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual]
    
    inline def inRange: typings.agGridCommunity.agGridCommunityStrings.inRange = "inRange".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.inRange]
    
    inline def lastWeek: typings.agGridCommunity.agGridCommunityStrings.lastWeek = "lastWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lastWeek]
    
    inline def lessThan: typings.agGridCommunity.agGridCommunityStrings.lessThan = "lessThan".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lessThan]
    
    inline def lessThanOrEqual: typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual = "lessThanOrEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual]
    
    inline def nextWeek: typings.agGridCommunity.agGridCommunityStrings.nextWeek = "nextWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.nextWeek]
    
    inline def notBlank: typings.agGridCommunity.agGridCommunityStrings.notBlank = "notBlank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notBlank]
    
    inline def notEqual: typings.agGridCommunity.agGridCommunityStrings.notEqual = "notEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notEqual]
    
    inline def thisWeek: typings.agGridCommunity.agGridCommunityStrings.thisWeek = "thisWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.thisWeek]
    
    inline def today: typings.agGridCommunity.agGridCommunityStrings.today = "today".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.today]
    
    inline def tomorrow: typings.agGridCommunity.agGridCommunityStrings.tomorrow = "tomorrow".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.tomorrow]
    
    inline def yesterday: typings.agGridCommunity.agGridCommunityStrings.yesterday = "yesterday".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.yesterday]
  }
  
  type FilterOptionKey = ISimpleFilterModelType | CustomFilterOptionKey
  
  type FilterPlaceholderFunction = js.Function1[/* params */ IFilterPlaceholderFunctionParams, String]
  
  trait ICombinedSimpleModel[M /* <: ISimpleFilterModel */]
    extends StObject
       with ProvidedFilterModel {
    
    var conditions: js.Array[M]
    
    var operator: JoinOperator
  }
  object ICombinedSimpleModel {
    
    inline def apply[M /* <: ISimpleFilterModel */](conditions: js.Array[M], operator: JoinOperator): ICombinedSimpleModel[M] = {
      val __obj = js.Dynamic.literal(conditions = conditions.asInstanceOf[js.Any], operator = operator.asInstanceOf[js.Any])
      __obj.asInstanceOf[ICombinedSimpleModel[M]]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: ICombinedSimpleModel[?], M /* <: ISimpleFilterModel */] (val x: Self & ICombinedSimpleModel[M]) extends AnyVal {
      
      inline def setConditions(value: js.Array[M]): Self = StObject.set(x, "conditions", value.asInstanceOf[js.Any])
      
      inline def setConditionsVarargs(value: M*): Self = StObject.set(x, "conditions", js.Array(value*))
      
      inline def setOperator(value: JoinOperator): Self = StObject.set(x, "operator", value.asInstanceOf[js.Any])
    }
  }
  
  trait IFilterPlaceholderFunctionParams extends StObject {
    
    /** The filter option name as localised text */
    var filterOption: String
    
    /** The filter option key. A Custom Filter Option reports its `displayKey`. */
    var filterOptionKey: FilterOptionKey
    
    /** The default placeholder text */
    var placeholder: String
  }
  object IFilterPlaceholderFunctionParams {
    
    inline def apply(filterOption: String, filterOptionKey: FilterOptionKey, placeholder: String): IFilterPlaceholderFunctionParams = {
      val __obj = js.Dynamic.literal(filterOption = filterOption.asInstanceOf[js.Any], filterOptionKey = filterOptionKey.asInstanceOf[js.Any], placeholder = placeholder.asInstanceOf[js.Any])
      __obj.asInstanceOf[IFilterPlaceholderFunctionParams]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: IFilterPlaceholderFunctionParams] (val x: Self) extends AnyVal {
      
      inline def setFilterOption(value: String): Self = StObject.set(x, "filterOption", value.asInstanceOf[js.Any])
      
      inline def setFilterOptionKey(value: FilterOptionKey): Self = StObject.set(x, "filterOptionKey", value.asInstanceOf[js.Any])
      
      inline def setPlaceholder(value: String): Self = StObject.set(x, "placeholder", value.asInstanceOf[js.Any])
    }
  }
  
  trait ISimpleFilterModel
    extends StObject
       with ProvidedFilterModel {
    
    /** One of the filter options, e.g. `'equals'`, or a Custom Filter Option's `displayKey`. */
    var `type`: js.UndefOr[FilterOptionKey | Null] = js.undefined
  }
  object ISimpleFilterModel {
    
    inline def apply(): ISimpleFilterModel = {
      val __obj = js.Dynamic.literal()
      __obj.asInstanceOf[ISimpleFilterModel]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: ISimpleFilterModel] (val x: Self) extends AnyVal {
      
      inline def setType(value: FilterOptionKey): Self = StObject.set(x, "type", value.asInstanceOf[js.Any])
      
      inline def setTypeNull: Self = StObject.set(x, "type", null)
      
      inline def setTypeUndefined: Self = StObject.set(x, "type", js.undefined)
    }
  }
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.today
    - typings.agGridCommunity.agGridCommunityStrings.yesterday
    - typings.agGridCommunity.agGridCommunityStrings.tomorrow
    - typings.agGridCommunity.agGridCommunityStrings.thisWeek
    - typings.agGridCommunity.agGridCommunityStrings.lastWeek
    - typings.agGridCommunity.agGridCommunityStrings.nextWeek
  */
  trait ISimpleFilterModelPresetType extends StObject
  object ISimpleFilterModelPresetType {
    
    inline def lastWeek: typings.agGridCommunity.agGridCommunityStrings.lastWeek = "lastWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lastWeek]
    
    inline def nextWeek: typings.agGridCommunity.agGridCommunityStrings.nextWeek = "nextWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.nextWeek]
    
    inline def thisWeek: typings.agGridCommunity.agGridCommunityStrings.thisWeek = "thisWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.thisWeek]
    
    inline def today: typings.agGridCommunity.agGridCommunityStrings.today = "today".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.today]
    
    inline def tomorrow: typings.agGridCommunity.agGridCommunityStrings.tomorrow = "tomorrow".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.tomorrow]
    
    inline def yesterday: typings.agGridCommunity.agGridCommunityStrings.yesterday = "yesterday".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.yesterday]
  }
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.equals
    - typings.agGridCommunity.agGridCommunityStrings.notEqual
    - typings.agGridCommunity.agGridCommunityStrings.contains
    - typings.agGridCommunity.agGridCommunityStrings.notContains
    - typings.agGridCommunity.agGridCommunityStrings.startsWith
    - typings.agGridCommunity.agGridCommunityStrings.endsWith
    - typings.agGridCommunity.agGridCommunityStrings.empty
    - typings.agGridCommunity.agGridCommunityStrings.blank
    - typings.agGridCommunity.agGridCommunityStrings.notBlank
    - typings.agGridCommunity.agGridCommunityStrings.lessThan
    - typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual
    - typings.agGridCommunity.agGridCommunityStrings.greaterThan
    - typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual
    - typings.agGridCommunity.agGridCommunityStrings.inRange
    - typings.agGridCommunity.agGridCommunityStrings.today
    - typings.agGridCommunity.agGridCommunityStrings.yesterday
    - typings.agGridCommunity.agGridCommunityStrings.tomorrow
    - typings.agGridCommunity.agGridCommunityStrings.thisWeek
    - typings.agGridCommunity.agGridCommunityStrings.lastWeek
    - typings.agGridCommunity.agGridCommunityStrings.nextWeek
  */
  trait ISimpleFilterModelType extends StObject
  object ISimpleFilterModelType {
    
    inline def blank: typings.agGridCommunity.agGridCommunityStrings.blank = "blank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.blank]
    
    inline def contains: typings.agGridCommunity.agGridCommunityStrings.contains = "contains".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.contains]
    
    inline def empty: typings.agGridCommunity.agGridCommunityStrings.empty = "empty".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.empty]
    
    inline def endsWith: typings.agGridCommunity.agGridCommunityStrings.endsWith = "endsWith".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.endsWith]
    
    inline def equals: typings.agGridCommunity.agGridCommunityStrings.equals = "equals".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.equals]
    
    inline def greaterThan: typings.agGridCommunity.agGridCommunityStrings.greaterThan = "greaterThan".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.greaterThan]
    
    inline def greaterThanOrEqual: typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual = "greaterThanOrEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual]
    
    inline def inRange: typings.agGridCommunity.agGridCommunityStrings.inRange = "inRange".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.inRange]
    
    inline def lastWeek: typings.agGridCommunity.agGridCommunityStrings.lastWeek = "lastWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lastWeek]
    
    inline def lessThan: typings.agGridCommunity.agGridCommunityStrings.lessThan = "lessThan".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lessThan]
    
    inline def lessThanOrEqual: typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual = "lessThanOrEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual]
    
    inline def nextWeek: typings.agGridCommunity.agGridCommunityStrings.nextWeek = "nextWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.nextWeek]
    
    inline def notBlank: typings.agGridCommunity.agGridCommunityStrings.notBlank = "notBlank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notBlank]
    
    inline def notContains: typings.agGridCommunity.agGridCommunityStrings.notContains = "notContains".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notContains]
    
    inline def notEqual: typings.agGridCommunity.agGridCommunityStrings.notEqual = "notEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notEqual]
    
    inline def startsWith: typings.agGridCommunity.agGridCommunityStrings.startsWith = "startsWith".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.startsWith]
    
    inline def thisWeek: typings.agGridCommunity.agGridCommunityStrings.thisWeek = "thisWeek".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.thisWeek]
    
    inline def today: typings.agGridCommunity.agGridCommunityStrings.today = "today".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.today]
    
    inline def tomorrow: typings.agGridCommunity.agGridCommunityStrings.tomorrow = "tomorrow".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.tomorrow]
    
    inline def yesterday: typings.agGridCommunity.agGridCommunityStrings.yesterday = "yesterday".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.yesterday]
  }
  
  trait ISimpleFilterParams extends StObject {
    
    var defaultJoinOperator: js.UndefOr[JoinOperator] = js.undefined
    
    /** The default filter option to be selected. Must be one of the offered options. */
    var defaultOption: js.UndefOr[ISimpleFilterModelType | CustomFilterOptionKey] = js.undefined
    
    var filterPlaceholder: js.UndefOr[FilterPlaceholderFunction | String] = js.undefined
  }
  object ISimpleFilterParams {
    
    inline def apply(): ISimpleFilterParams = {
      val __obj = js.Dynamic.literal()
      __obj.asInstanceOf[ISimpleFilterParams]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: ISimpleFilterParams] (val x: Self) extends AnyVal {
      
      inline def setDefaultJoinOperator(value: JoinOperator): Self = StObject.set(x, "defaultJoinOperator", value.asInstanceOf[js.Any])
      
      inline def setDefaultJoinOperatorUndefined: Self = StObject.set(x, "defaultJoinOperator", js.undefined)
      
      inline def setDefaultOption(value: ISimpleFilterModelType | CustomFilterOptionKey): Self = StObject.set(x, "defaultOption", value.asInstanceOf[js.Any])
      
      inline def setDefaultOptionUndefined: Self = StObject.set(x, "defaultOption", js.undefined)
      
      inline def setFilterPlaceholder(value: FilterPlaceholderFunction | String): Self = StObject.set(x, "filterPlaceholder", value.asInstanceOf[js.Any])
      
      inline def setFilterPlaceholderFunction1(value: /* params */ IFilterPlaceholderFunctionParams => String): Self = StObject.set(x, "filterPlaceholder", js.Any.fromFunction1(value))
      
      inline def setFilterPlaceholderUndefined: Self = StObject.set(x, "filterPlaceholder", js.undefined)
    }
  }
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.AND
    - typings.agGridCommunity.agGridCommunityStrings.OR
  */
  trait JoinOperator extends StObject
  object JoinOperator {
    
    inline def AND: typings.agGridCommunity.agGridCommunityStrings.AND = "AND".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.AND]
    
    inline def OR: typings.agGridCommunity.agGridCommunityStrings.OR = "OR".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.OR]
  }
  
  trait ProvidedFilterModel extends StObject {
    
    var filterType: js.UndefOr[String] = js.undefined
  }
  object ProvidedFilterModel {
    
    inline def apply(): ProvidedFilterModel = {
      val __obj = js.Dynamic.literal()
      __obj.asInstanceOf[ProvidedFilterModel]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: ProvidedFilterModel] (val x: Self) extends AnyVal {
      
      inline def setFilterType(value: String): Self = StObject.set(x, "filterType", value.asInstanceOf[js.Any])
      
      inline def setFilterTypeUndefined: Self = StObject.set(x, "filterType", js.undefined)
    }
  }
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.equals
    - typings.agGridCommunity.agGridCommunityStrings.notEqual
    - typings.agGridCommunity.agGridCommunityStrings.lessThan
    - typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual
    - typings.agGridCommunity.agGridCommunityStrings.greaterThan
    - typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual
    - typings.agGridCommunity.agGridCommunityStrings.inRange
    - typings.agGridCommunity.agGridCommunityStrings.empty
    - typings.agGridCommunity.agGridCommunityStrings.blank
    - typings.agGridCommunity.agGridCommunityStrings.notBlank
  */
  trait ScalarFilterOptionKey extends StObject
  object ScalarFilterOptionKey {
    
    inline def blank: typings.agGridCommunity.agGridCommunityStrings.blank = "blank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.blank]
    
    inline def empty: typings.agGridCommunity.agGridCommunityStrings.empty = "empty".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.empty]
    
    inline def equals: typings.agGridCommunity.agGridCommunityStrings.equals = "equals".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.equals]
    
    inline def greaterThan: typings.agGridCommunity.agGridCommunityStrings.greaterThan = "greaterThan".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.greaterThan]
    
    inline def greaterThanOrEqual: typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual = "greaterThanOrEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.greaterThanOrEqual]
    
    inline def inRange: typings.agGridCommunity.agGridCommunityStrings.inRange = "inRange".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.inRange]
    
    inline def lessThan: typings.agGridCommunity.agGridCommunityStrings.lessThan = "lessThan".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lessThan]
    
    inline def lessThanOrEqual: typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual = "lessThanOrEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.lessThanOrEqual]
    
    inline def notBlank: typings.agGridCommunity.agGridCommunityStrings.notBlank = "notBlank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notBlank]
    
    inline def notEqual: typings.agGridCommunity.agGridCommunityStrings.notEqual = "notEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notEqual]
  }
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.value
    - typings.agGridCommunity.agGridCommunityStrings.percentOfTotal
    - typings.agGridCommunity.agGridCommunityStrings.percentOfRow
  */
  trait ShowValuesAsBuiltInType extends StObject
  object ShowValuesAsBuiltInType {
    
    inline def percentOfRow: typings.agGridCommunity.agGridCommunityStrings.percentOfRow = "percentOfRow".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.percentOfRow]
    
    inline def percentOfTotal: typings.agGridCommunity.agGridCommunityStrings.percentOfTotal = "percentOfTotal".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.percentOfTotal]
    
    inline def value: typings.agGridCommunity.agGridCommunityStrings.value = "value".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.value]
  }
  
  trait ShowValuesAsParams extends StObject {
    
    var showValuesAs: ShowValuesAsType
    
    var transformedDataType: js.UndefOr[BaseCellDataType | String] = js.undefined
  }
  object ShowValuesAsParams {
    
    inline def apply(showValuesAs: ShowValuesAsType): ShowValuesAsParams = {
      val __obj = js.Dynamic.literal(showValuesAs = showValuesAs.asInstanceOf[js.Any])
      __obj.asInstanceOf[ShowValuesAsParams]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: ShowValuesAsParams] (val x: Self) extends AnyVal {
      
      inline def setShowValuesAs(value: ShowValuesAsType): Self = StObject.set(x, "showValuesAs", value.asInstanceOf[js.Any])
      
      inline def setTransformedDataType(value: BaseCellDataType | String): Self = StObject.set(x, "transformedDataType", value.asInstanceOf[js.Any])
      
      inline def setTransformedDataTypeUndefined: Self = StObject.set(x, "transformedDataType", js.undefined)
    }
  }
  
  type ShowValuesAsType = ShowValuesAsBuiltInType | String
  
  /* Rewritten from type alias, can be one of: 
    - typings.agGridCommunity.agGridCommunityStrings.equals
    - typings.agGridCommunity.agGridCommunityStrings.notEqual
    - typings.agGridCommunity.agGridCommunityStrings.contains
    - typings.agGridCommunity.agGridCommunityStrings.notContains
    - typings.agGridCommunity.agGridCommunityStrings.startsWith
    - typings.agGridCommunity.agGridCommunityStrings.endsWith
    - typings.agGridCommunity.agGridCommunityStrings.empty
    - typings.agGridCommunity.agGridCommunityStrings.blank
    - typings.agGridCommunity.agGridCommunityStrings.notBlank
  */
  trait TextFilterOptionKey extends StObject
  object TextFilterOptionKey {
    
    inline def blank: typings.agGridCommunity.agGridCommunityStrings.blank = "blank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.blank]
    
    inline def contains: typings.agGridCommunity.agGridCommunityStrings.contains = "contains".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.contains]
    
    inline def empty: typings.agGridCommunity.agGridCommunityStrings.empty = "empty".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.empty]
    
    inline def endsWith: typings.agGridCommunity.agGridCommunityStrings.endsWith = "endsWith".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.endsWith]
    
    inline def equals: typings.agGridCommunity.agGridCommunityStrings.equals = "equals".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.equals]
    
    inline def notBlank: typings.agGridCommunity.agGridCommunityStrings.notBlank = "notBlank".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notBlank]
    
    inline def notContains: typings.agGridCommunity.agGridCommunityStrings.notContains = "notContains".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notContains]
    
    inline def notEqual: typings.agGridCommunity.agGridCommunityStrings.notEqual = "notEqual".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.notEqual]
    
    inline def startsWith: typings.agGridCommunity.agGridCommunityStrings.startsWith = "startsWith".asInstanceOf[typings.agGridCommunity.agGridCommunityStrings.startsWith]
  }
}
