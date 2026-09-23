/** Marks a collection of many rows by the absence of a single-result flag. */
export interface NonSingleResult {
    singleResult?: never;
}

/** Marks a collection that answers one row. */
export interface SingleResult {
    singleResult: true;
}

export interface CollectionConfig<T> {
    getKey: (item: T) => string;
}

export interface Collection<T> {
    get(key: string): T | undefined;
}

/**
 * Overloads told apart only by an intersected marker erase alike, so they combine into one method. An argument
 * satisfies one overload's intersection or the other's, never both, so the combined parameter is their union.
 */
export declare function createCollection<T extends object>(options: CollectionConfig<T> & {
    schema?: never;
} & NonSingleResult): Collection<T> & NonSingleResult;
export declare function createCollection<T extends object>(options: CollectionConfig<T> & {
    schema?: never;
} & SingleResult): Collection<T> & SingleResult;

export interface Signal {
    aborted: boolean;
}

/** The idiom for an omitted `TPage = never` type argument: which branch applies is known once `TPage` is. */
export type QueryContext<TKey, TPage = never> = [TPage] extends [never] ? {
    queryKey: TKey;
    signal: Signal;
} : {
    queryKey: TKey;
    signal: Signal;
    pageParam: TPage;
};

export declare function query(fn: (context: QueryContext<string>) => void): void;
export declare function pagedQuery(fn: (context: QueryContext<string, number>) => void): void;

export interface ObserverOptions {
    retry: boolean | number;
    refetchInterval: number | false;
}

/** A template literal without substitutions is the string it spells, so it can key a lookup. */
export declare function retrying(retry: ObserverOptions[`retry`]): void;
export declare function refetching(interval: ObserverOptions[`refetchInterval`]): void;
