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
import { FetchRequest } from './FetchAdapter';
import ITokenAuthenticationHelper from './ITokenAuthenticationHelper';

export class FetchRequestWrapperReact implements FetchRequest {
    private baseUrl = "";
    private authHelper: ITokenAuthenticationHelper | undefined;

    private fetchFunction: (url: string | Request , options: RequestInit | undefined) => Promise<Response>;
    constructor(baseUrl?: string, authHelper?: ITokenAuthenticationHelper) {
        this.authHelper = authHelper;
        this.baseUrl = baseUrl || this.baseUrl;
        this.fetchFunction = window.fetch.bind(window);
        this.fetch = this.fetch.bind(this);
    }

    public async fetch(url: string | Request | undefined, options?: RequestInit, dontApplyBasePath?: boolean): Promise<Response> {
        if (url === undefined) { throw new Error("No Url has been passed"); }

        const request = typeof url === "string" ? this.createUrl(url, dontApplyBasePath) : { ...url, url: this.createUrl(url.url, dontApplyBasePath) };
        const opts = await this.addHeaders(options);

        try {
            const result = await this.fetchFunction(request, opts);
            if (result.status === 401 && this.authHelper && await this.authHelper.getAccessToken()) {
                try {
                    const optsWithRefreshedToken = await this.addHeaders(options, true);
                    const result2 = await this.fetchFunction(request, optsWithRefreshedToken);
                    return result2;
                } catch (err) {
                    // tslint:disable-next-line:no-console
                    console.error("Failed to refresh AccessToken", err);
                    return result;
                }
            }
            return result
        } catch (err) {
            // tslint:disable-next-line:no-console
            console.log(err);
            throw err;
        }

    }

    public get(url: string, options?: RequestInit): Promise<Response> {
        return this.fetch((url), { method: "get", ...(options || {}) });
    }
    public delete(url: string, options?: RequestInit): Promise<Response> {
        return this.fetch((url), { method: "delete", ...(options || {}) });
    }
    public head(url: string, options?: RequestInit): Promise<Response> {
        return this.fetch((url), { method: "head", ...(options || {}) });
    }
    public options(url: string, options?: RequestInit): Promise<Response> {
        return this.fetch((url), { method: "options", ...(options || {}) });
    }
    public post(url: string, options?: RequestInit): Promise<Response> {
        return this.fetch((url), { method: "post", ...(options || {}) });
    }
    public put(url: string, options?: RequestInit): Promise<Response> {
        return this.fetch((url), { method: "put", ...(options || {}) });
    }
    public patch(url: string, options?: RequestInit): Promise<Response> {
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

    private async addHeaders(options?: RequestInit, performRefresh?: boolean): Promise<RequestInit | undefined> {
        if (this.authHelper) {
            const { headers } = options || { headers: {} };
            if (headers &&
                (headers as string[][]).concat &&
                (headers as string[][]).slice &&
                (headers as string[][]).length) {
                // headers is probably array
                const nextHeaders = (headers as string[][]).slice();

                // TODO: #FIXME
                // tslint:disable-next-line:no-console
                console.error("Will not apply auth headers to string array");
                return { ...(options || {}), headers: nextHeaders };
            } else {
                const nextHeaders: Record<string, string> = { ...(headers || {}) } as Record<string, string>;
                if (!(nextHeaders).Authorization) {

                    const accessToken: string | undefined = performRefresh ? await this.authHelper.refreshAccessToken() : await this.authHelper.getAccessToken();
                    if (accessToken) { nextHeaders.Authorization = `Bearer ${accessToken}`; }
                }
                return { ...(options || {}), headers: nextHeaders };
            }
        }
        return options;
    }
}

export default FetchRequestWrapperReact;