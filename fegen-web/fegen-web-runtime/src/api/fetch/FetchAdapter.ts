/**
 * Copyright 2020 Materna Information & Communications SE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import {FetchFn, FetchOptions, FetchResponse} from "./FetchTypes";
import {Interceptor} from "./Interceptor";

export interface FetchAdapterOptions {
    baseUrl?: string;
    interceptors?: Interceptor[];
    fetchImpl?: FetchFn
}

export class FetchAdapter {

    readonly baseUrl: string;

    private readonly fetchImpl: FetchFn;

    constructor(options?: FetchAdapterOptions) {
        this.baseUrl = options?.baseUrl ?? "";
        const fetchImpl = options?.fetchImpl ?? window.fetch.bind(window);
        this.fetchImpl = FetchAdapter.wrapInterceptors(fetchImpl, options?.interceptors ?? []);
        this.fetch = this.fetch.bind(this);
    }

    private static wrapInterceptors(fetchFn: FetchFn, interceptors: Interceptor[]): FetchFn {
        let result = fetchFn;
        for (const interceptor of interceptors.reverse() ?? []) {
            result = FetchAdapter.wrapInterceptor(result, interceptor);
        }
        return result;
    }

    private static wrapInterceptor(fetchFn: FetchFn, interceptor: Interceptor): FetchFn {
        return (url, options) => {
            return interceptor(url, options, fetchFn);
        }
    }

    public async fetch(url: string, options?: FetchOptions, dontApplyBasePath?: boolean): Promise<FetchResponse> {
        return await this.fetchImpl(this.createUrl(url, dontApplyBasePath), options);
    }

    public get(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "get", ...(options || {}) });
    }
    public delete(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "delete", ...(options || {}) });
    }
    public head(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "head", ...(options || {}) });
    }
    public options(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "options", ...(options || {}) });
    }
    public post(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "post", ...(options || {}) });
    }
    public put(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "put", ...(options || {}) });
    }
    public patch(url: string, options?: FetchOptions): Promise<FetchResponse> {
        return this.fetch((url), { method: "patch", ...(options || {}) });
    }

    private createUrl(url: string, dontApplyBasePath: boolean = false) {
        if (!this.baseUrl || !url || url.toLowerCase().indexOf("://") >= 0) {
            return url;
        }
        let urlWithoutLeadingSlash = url;
        while (urlWithoutLeadingSlash.indexOf("/") === 0) {
            urlWithoutLeadingSlash = urlWithoutLeadingSlash.slice(1);
        }
        let baseUrl = this.baseUrl;
        if (dontApplyBasePath) {
            let domainStart = baseUrl.indexOf("://") + 3;
            if (domainStart == -1) {
                domainStart = 0;
            }
            let pathStart = baseUrl.indexOf("/", domainStart);
            if (pathStart == -1) {
                pathStart = 0;
            }
            baseUrl = baseUrl.slice(0, pathStart);
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return `${baseUrl}${urlWithoutLeadingSlash}`;

    }
}

export default FetchAdapter;