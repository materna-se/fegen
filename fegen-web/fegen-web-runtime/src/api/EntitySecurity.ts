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
import {extractSecurityResponse} from "./EndpointSecurity";


export default class EntitySecurity {

    constructor(
        readonly readOne: boolean,
        readonly readAll: boolean,
        readonly create: boolean,
        readonly update: boolean,
        readonly remove: boolean
    ) {}

    static async fetch(fetchAdapter: FetchAdapter, basePath: string, entityPath: string): Promise<EntitySecurity> {
        const generalSecurity = await this.fetchAllowedMethods(fetchAdapter, basePath, entityPath);
        const specificSecurity = await this.fetchAllowedMethods(fetchAdapter, basePath, `${entityPath}/1`);

        return new EntitySecurity(
            specificSecurity.indexOf("GET") !== -1,
            generalSecurity.indexOf("GET") !== -1,
            generalSecurity.indexOf("POST") !== -1,
            specificSecurity.indexOf("PUT") !== -1,
            specificSecurity.indexOf("DELETE") !== -1
        )
    }

    private static async fetchAllowedMethods(fetchAdapter: FetchAdapter, basePath: string, path: string): Promise<string[]> {
        const url = `${basePath}/fegen/security/allowedMethods?path=${path}`;
        let response;
        try {
            response = await fetchAdapter.get(url);
        } catch (ex) {
            throw new Error(`Failed to fetch security configuration at ${url}: ${ex}`);
        }
        return extractSecurityResponse(url, response);
    }
}
