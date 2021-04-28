

export type FetchHeaders = {
    append(name: string, value: string): void;
    delete(name: string): void;
    get(name: string): string | null;
    has(name: string): boolean;
    set(name: string, value: string): void;
};

export type FetchOptions = {
    body?: string,
    headers?: Record<string, string>,
    method?: string
};

export type FetchResponse = {
    readonly headers: FetchHeaders;
    readonly ok: boolean;
    readonly redirected: boolean;
    readonly status: number;
    readonly statusText: string;
    json(): Promise<any>;
    text(): Promise<string>;
};

export type FetchFn = (url: string , options?: FetchOptions) => Promise<FetchResponse>;