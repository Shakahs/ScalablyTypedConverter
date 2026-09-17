// Reduced from ag-grid-community 36.2.0, dist/types/src/filter/provided/iSimpleFilter.d.ts and
// dist/types/src/entities/colDef-showValuesAs.d.ts.
//
// `string & Record<never, never>` and `string & {}` are the "autocomplete-friendly string" idiom: a union of
// string literals which still accepts any string. Both must become plain `String` in Scala; the Scala 3
// compiler crashes on `String & Record[Nothing, Nothing]` inside a union.

export interface ProvidedFilterModel {
    filterType?: string;
}
export type JoinOperator = 'AND' | 'OR';

export interface IFilterPlaceholderFunctionParams {
    /** The filter option key. A Custom Filter Option reports its `displayKey`. */
    filterOptionKey: FilterOptionKey;
    /** The filter option name as localised text */
    filterOption: string;
    /** The default placeholder text */
    placeholder: string;
}
export type FilterPlaceholderFunction = (params: IFilterPlaceholderFunctionParams) => string;

export interface ISimpleFilterParams {
    /** The default filter option to be selected. Must be one of the offered options. */
    defaultOption?: ISimpleFilterModelType | CustomFilterOptionKey;
    defaultJoinOperator?: JoinOperator;
    filterPlaceholder?: FilterPlaceholderFunction | string;
}

export type ISimpleFilterModelPresetType = 'today' | 'yesterday' | 'tomorrow' | 'thisWeek' | 'lastWeek' | 'nextWeek';
/** The `displayKey` of a Custom Filter Option. */
export type CustomFilterOptionKey = string & Record<never, never>;
/** A built-in filter option key, or the `displayKey` of a Custom Filter Option. */
export type FilterOptionKey = ISimpleFilterModelType | CustomFilterOptionKey;
export type CommonFilterOptionKey = 'empty' | 'blank' | 'notBlank';
export type TextFilterOptionKey = CommonFilterOptionKey | 'equals' | 'notEqual' | 'contains' | 'notContains' | 'startsWith' | 'endsWith';
export type ScalarFilterOptionKey = CommonFilterOptionKey | 'equals' | 'notEqual' | 'lessThan' | 'lessThanOrEqual' | 'greaterThan' | 'greaterThanOrEqual' | 'inRange';
export type DateFilterOptionKey = ScalarFilterOptionKey | ISimpleFilterModelPresetType;
export type ISimpleFilterModelType = TextFilterOptionKey | DateFilterOptionKey;

export interface ISimpleFilterModel extends ProvidedFilterModel {
    /** One of the filter options, e.g. `'equals'`, or a Custom Filter Option's `displayKey`. */
    type?: FilterOptionKey | null;
}
export interface ICombinedSimpleModel<M extends ISimpleFilterModel> extends ProvidedFilterModel {
    operator: JoinOperator;
    conditions: M[];
}
export declare function isCombinedFilterModel<M extends ISimpleFilterModel>(model: M | ICombinedSimpleModel<M>): model is ICombinedSimpleModel<M>;

export type ShowValuesAsBuiltInType = 'value' | 'percentOfTotal' | 'percentOfRow';
export type ShowValuesAsType = ShowValuesAsBuiltInType | (string & {});
export type BaseCellDataType = 'text' | 'number' | 'boolean';
export interface ShowValuesAsParams {
    showValuesAs: ShowValuesAsType;
    transformedDataType?: BaseCellDataType | (string & {});
}
