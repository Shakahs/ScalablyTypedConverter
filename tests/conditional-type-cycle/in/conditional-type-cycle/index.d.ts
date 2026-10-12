// The shape of @tanstack/db 0.13's query builder result types: each conditional type is
// approximated on its own, and the branch ResultTypeFromSelect takes for a direct ref leads back
// to ResultTypeFromSelect through ExtractDirectSelectRef and ExtractRef.

interface Ref<T> {
  readonly __ref: T;
}

type IsTrueRef<T> = T extends Ref<infer _U> ? true : false;

type Prettify<T> = {
  [K in keyof T]: T[K];
} & {};

type EmptySelectedRef<T> = T extends object ? {
  [K in keyof T]?: never;
} : never;

export type ResultTypeFromSelect<TSelectObject> = IsTrueRef<TSelectObject> extends true ? ExtractDirectSelectRef<TSelectObject> : {
  [K in keyof TSelectObject]: TSelectObject[K] extends Ref<infer T> ? T : ResultTypeFromSelect<TSelectObject[K]>;
};

type ExtractRef<T> = T extends Ref<infer U> ? U : Prettify<ResultTypeFromSelect<T>>;

type ExtractDirectSelectRef<T> = NonNullable<ExtractRef<T>> | EmptySelectedRef<NonNullable<ExtractRef<T>>>;

export declare function select<TSelectObject>(selectObject: TSelectObject): ResultTypeFromSelect<TSelectObject>;
