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
import {FetchAdapter} from "./fetch/FetchAdapter";
import {FetchResponse} from "./fetch/FetchTypes";

export const extractSecurityResponse = (url: string, response: FetchResponse): any => {
    if (response.ok) {
        return response.json();
    } else if (response.status === 404) {
        throw new Error("Security endpoint not found. Make sure you included the FeGen utils dependency and Spring Security in your backend and annotated your application with @Fegen");
    } else {
        throw new Error(`Failed to fetch security configuration at ${url}: Server returned error code ${response.status}`);
    }
}

export const isEndpointCallAllowed = async (fetchAdapter: FetchAdapter, basePath: string, method: string, path: string): Promise<boolean> => {
    const url = `${basePath}/fegen/security/isAllowed?method=${method}&path=${path}`;
    let response;
    try {
        response = await fetchAdapter.get(url);
    } catch (ex) {
        throw new Error(`Failed to fetch security configuration at ${url}: ${ex}`);
    }
    return extractSecurityResponse(url, response);
}