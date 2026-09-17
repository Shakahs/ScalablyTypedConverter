/// <reference no-default-lib="true"/>

interface Array<T> {}
type Record<K extends keyof any, T> = {
    [P in K]: T;
};
