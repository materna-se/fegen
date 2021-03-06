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
import stringHelper from '../helpers/stringHelper';
import apiHelper from './ApiHelper';
import {
    ApiHateoasObjectReadMultiple,
    Dto, Entity,
    PagedItems, WithId
} from './ApiTypes';
import FetchAdapter from './fetch/FetchAdapter';
import {FetchResponse} from "./fetch/FetchTypes";

export default class RequestAdapter {

    readonly fetchAdapter: FetchAdapter;

    constructor(fetchAdapter: FetchAdapter) {
        this.fetchAdapter = fetchAdapter;

        this.getPage = this.getPage.bind(this);

        this.createObject = this.createObject.bind(this);
        this.deleteObject = this.deleteObject.bind(this);
        this.updateObject = this.updateObject.bind(this);
    }

    public async getPage<T extends Dto>(url: string, embeddedPropName: string, projectionName?: string, page?: number, size?: number, sort?: string): Promise<PagedItems<T>> {
        const hasProjection = (!!projectionName);
        const hasPage = (!!page || page === 0);
        const hasSize = (!!size || size === 0);
        const hasSort = !!sort;

        let fullUrl = url;
        const urlContainsParams = !!fullUrl && fullUrl.indexOf("?") > 0;

        const firstConnector = urlContainsParams ? "&" : "?";

        fullUrl = hasProjection ? `${fullUrl}${firstConnector}projection=${projectionName}` : fullUrl;
        fullUrl = hasPage ? `${fullUrl}${hasProjection ? "&" : firstConnector}page=${page}` : fullUrl;
        fullUrl = hasSize ? `${fullUrl}${hasProjection || hasPage ? "&" : firstConnector}size=${size}` : fullUrl;
        fullUrl = hasSort ? `${fullUrl}${hasProjection || hasPage || hasSize ? "&" : firstConnector}sort=${sort}` : fullUrl;

        const response = await this.fetchAdapter.get(fullUrl);
        await RequestAdapter.assertOk(response);

        const obj = ((await response.json()) as ApiHateoasObjectReadMultiple<T[]>);
        const items = (obj._embedded[embeddedPropName]).map(item => (apiHelper.injectIds(item) as T));
        return {
            items,
            _links: obj._links,
            page: obj.page,
        };
    }

    public postJsonData(url: string, bodyContent: string): Promise<FetchResponse> {
        return this.fetchAdapter.post(url, {
            headers: {
                "content-type": "application/json"
            },
            body: bodyContent
        });
    }

    public async createObject<D extends Dto>(newObject: {}, createURI: string): Promise<WithId<D>> {
        const createResponse = await this.postJsonData(createURI, JSON.stringify(newObject));

        await RequestAdapter.assertOk(createResponse);

        const storedEntity: D = (await createResponse.json() as D);
        return apiHelper.injectIds(storedEntity);
    }

    public async updateObject<T extends Dto>(obj: T): Promise<T & Entity> {
        const response = await this.fetchAdapter.put(apiHelper.removeParamsFromNavigationHref(obj._links.self), {
            headers: {
                "content-type": "application/json"
            },
            body: JSON.stringify(obj)
        });
        await RequestAdapter.assertOk(response);

        const storedEntity: T = (await response.json() as T);
        return apiHelper.injectIds(storedEntity);
    }

    private static async assertOk(response: FetchResponse): Promise<void> {
        if (!response.ok) {
            const body = await response.text();
            throw Error(`Received ${response.status} - ${response.statusText} response: ${body}`)
        }
    }

    public async deleteObject<T extends Entity>(existingObject: T): Promise<void> {
        const response = await this.fetchAdapter.delete(apiHelper.removeParamsFromNavigationHref(existingObject._links.self));
        await RequestAdapter.assertOk(response);
    }

    public async adaptAnyToMany(
        baseURL: string,
        uris: string[]
    ): Promise<FetchResponse> {
        return await this.fetchAdapter.put(baseURL, {
            headers: {
                "content-type": "text/uri-list"
            },
            body: uris.join("\n")
        })
    }

    public async adaptAnyToOne(
        baseURL: string,
        uri: string
    ): Promise<FetchResponse> {
        return await this.adaptAnyToMany(baseURL, [uri])
    }

    public async removeObjectFromCollection<TRemove extends Entity, T extends Entity, K extends keyof T>(objToBeRemoved: TRemove, objectWithCollection: T, collectionPropertyName: K): Promise<void> {
        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";
        const hrefRemove = objToBeRemoved._links ? apiHelper.removeParamsFromNavigationHref(objToBeRemoved._links.self) : "";

        if (!href) {
            throw new Error("No href found");
        }

        if (!hrefRemove) {
            throw new Error("No href found to remove");
        }

        const assocUrl = stringHelper.getStringWithoutTrailingSlash(href) + "/" + collectionPropertyName;

        const uris: string[] = ((objectWithCollection[collectionPropertyName] as unknown) as Entity[])
            .filter(o => !!o._links && apiHelper.removeParamsFromNavigationHref(o._links.self) !== hrefRemove)
            .map(o => !!o._links && apiHelper.removeParamsFromNavigationHref(o._links.self)) as string[];

        const associationResponse = await this.fetchAdapter.put(assocUrl, {
            headers: {
                "content-type": "text/uri-list"
            },
            body: uris.join("\n")
        });
        await RequestAdapter.assertOk(associationResponse);
    }

    public async addToObj<TAdd extends Dto, T extends Entity, K extends keyof T["_links"]>(objToBeAdd: TAdd, objectWithCollection: T, property: K): Promise<void> {

        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";
        const hrefAdd = objToBeAdd._links ? apiHelper.removeParamsFromNavigationHref(objToBeAdd._links.self) : "";

        if (!href) {
            throw new Error("No href found");
        }

        if (!hrefAdd) {
            throw new Error("No href found to add");
        }

        const assocUrl = stringHelper.getStringWithoutTrailingSlash(href) + "/" + property;

        const associationResponse = await this.fetchAdapter.post(assocUrl, {
            headers: {
                "content-type": "text/uri-list"
            },
            body: hrefAdd,
        });
        await RequestAdapter.assertOk(associationResponse);
    }

    public async updateObjectCollection<TInner extends Entity, T extends Entity, K extends keyof T = keyof T>(nextCollection: TInner[], objectWithCollection: T, property: K): Promise<void> {

        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";

        if (!href) {
            throw new Error("No href found or empty hrefs in object list");
        }
        const hrefs = nextCollection.map(o => o._links ? apiHelper.removeParamsFromNavigationHref(o._links.self) : "");
        if (!hrefs || hrefs.filter(h => !h).length > 0) {
            throw new Error("No hrefs found or empty hrefs in object list");
        }

        const assocUrl = stringHelper.getStringWithoutTrailingSlash(href) + "/" + property;
        const associationResponse = await this.adaptAnyToMany(assocUrl, hrefs);
        await RequestAdapter.assertOk(associationResponse);
    }
}