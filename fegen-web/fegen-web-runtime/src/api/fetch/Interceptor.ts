import {FetchFn, FetchOptions, FetchResponse} from "./FetchTypes";

export type Interceptor = (url: string, options: FetchOptions | undefined, execution: FetchFn) => Promise<FetchResponse>;
