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
import {StringHelper} from '../helpers/stringHelper';
import apiHelper from './ApiHelper';
import {ApiBase, ApiHateoasObjectBase, ApiHateoasObjectReadMultiple, ApiNavigationLinks, PagedItems} from './ApiTypes';
import FetchRequestWrapperReact from './FetchRequestWrapperReact';
import ITokenAuthenticationHelper from './ITokenAuthenticationHelper';

const stringHelper = new StringHelper();

export default class RequestAdapter {
    public readonly baseUrl: string = "";
    private authHelper: ITokenAuthenticationHelper | undefined;

    constructor(baseUrl?: string, authHelper?: ITokenAuthenticationHelper) {
        this.authHelper = authHelper;
        this.baseUrl = baseUrl || this.baseUrl;

        this.getRequest = this.getRequest.bind(this);
        this.getPage = this.getPage.bind(this);

        this.createObject = this.createObject.bind(this);
        this.deleteObject = this.deleteObject.bind(this);
        this.updateObject = this.updateObject.bind(this);


        this.moveObjectUp = this.moveObjectUp.bind(this);
        this.moveObjectDown = this.moveObjectDown.bind(this);
        this.getObjectsCollection = this.getObjectsCollection.bind(this);

        this.createObjectAndAddWithAssociation = this.createObjectAndAddWithAssociation.bind(this);
        this.deleteObjectAndAssociation = this.deleteObjectAndAssociation.bind(this);
    }

    public getRequest() {
        return new FetchRequestWrapperReact(this.baseUrl, this.authHelper);
    }

    public async getPage<T extends ApiBase & { _links: ApiNavigationLinks }>(url: string, embeddedPropName: string, projectionName?: string, page?: number, size?: number, sort?: string): Promise<PagedItems<T>> {
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

        const response = await this.getRequest().get(fullUrl);
        if (!response.ok) {
            throw response;
        }
        const obj = ((await response.json()) as ApiHateoasObjectReadMultiple<T[]>);
        const items = (obj._embedded[embeddedPropName]).map(item => (apiHelper.injectIds(item) as T));
        return {
            items,
            _links: obj._links,
            page: obj.page,
        };
    }

    public async postJsonData(url: string, bodyContent: string) {
        const createResponse = await this.getRequest().post(url, {
            headers: {
                "content-type": "application/json"
            },
            body: bodyContent
        });

        return createResponse;
    }

    public async createObject<TNew extends ApiBase>(newObject: TNew, createURI: string): Promise<TNew & { id: number, _links: ApiNavigationLinks }> {
        const createResponse = await this.postJsonData(createURI, JSON.stringify(newObject));

        if (!createResponse.ok) {
            throw createResponse;
        }

        const storedEntity: TNew & { _links: ApiNavigationLinks } = (await createResponse.json() as TNew & { _links: ApiNavigationLinks });
        return apiHelper.injectIds(storedEntity);
    }

    public async updateObject<T extends ApiBase & { _links: ApiNavigationLinks }>(obj: T): Promise<T & { id: number }> {
        const objWithLink = apiHelper.ensureObjectHasLinks(obj);
        const response = await this.getRequest().put(apiHelper.removeParamsFromNavigationHref(objWithLink._links.self), {
            headers: {
                "content-type": "application/json"
            },
            body: JSON.stringify(objWithLink)
        });
        if (!response.ok) {
            throw response;
        }

        const storedEntity: T = (await response.json() as T);
        return apiHelper.injectIds(storedEntity);
    }

    public async deleteObject<T extends ApiBase>(existingObject: T): Promise<void> {
        const objWithLink = apiHelper.ensureObjectHasLinks(existingObject);
        const response = await this.getRequest().delete(apiHelper.removeParamsFromNavigationHref(objWithLink._links.self));
        if (!response.ok) {
            throw response;
        }
    }

    public async adaptAnyToMany(
        baseURL: string,
        uris: string[]
    ): Promise<Response> {
        return await this.getRequest().put(baseURL, {
            headers: {
                "content-type": "text/uri-list"
            },
            body: uris.join("\n")
        })
    }

    public async adaptAnyToOne(
        baseURL: string,
        uri: string
    ): Promise<Response> {
        return await this.adaptAnyToMany(baseURL, [uri])
    }

    public async removeObjectFromCollection<TRemove extends ApiBase, T extends ApiBase, K extends keyof T>(objToBeRemoved: TRemove, objectWithCollection: T, collectionPropertyName: K): Promise<void> {
        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";
        const hrefRemove = objToBeRemoved._links ? apiHelper.removeParamsFromNavigationHref(objToBeRemoved._links.self) : "";

        if (!href) {
            throw new Error("No href found");
        }

        if (!hrefRemove) {
            throw new Error("No href found to remove");
        }

        const assocUrl = stringHelper.getStringWitoutTrailingSlash(href) + "/" + collectionPropertyName;

        const uris: string[] = ((objectWithCollection[collectionPropertyName] as any) as ApiBase[])
            .filter(o => !!o._links && apiHelper.removeParamsFromNavigationHref(o._links.self) !== hrefRemove)
            .map(o => !!o._links && apiHelper.removeParamsFromNavigationHref(o._links.self)) as string[];

        const associationResponse = await this.getRequest().put(assocUrl, {
            headers: {
                "content-type": "text/uri-list"
            },
            body: uris.join("\n")
        });
        if (!associationResponse.ok) {
            throw new Error(associationResponse.statusText + ` (${associationResponse.status})`);
        }
    }

    public async addToObj<TAdd extends ApiBase, T extends ApiBase, K extends keyof T["_links"]>(objToBeAdd: TAdd, objectWithCollection: T, property: K): Promise<void> {

        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";
        const hrefAdd = objToBeAdd._links ? apiHelper.removeParamsFromNavigationHref(objToBeAdd._links.self) : "";

        if (!href) {
            throw new Error("No href found");
        }

        if (!hrefAdd) {
            throw new Error("No href found to add");
        }

        const assocUrl = stringHelper.getStringWitoutTrailingSlash(href) + "/" + property;

        const associationResponse = await this.getRequest().post(assocUrl, {
            headers: {
                "content-type": "text/uri-list"
            },
            body: hrefAdd,
        });
        if (!associationResponse.ok) {
            throw new Error(associationResponse.statusText + ` (${associationResponse.status})`);
        }
    }

    public async updateObjectCollection<TInner extends ApiBase, T extends ApiBase, K extends keyof T = keyof T>(nextCollection: TInner[], objectWithCollection: T, property: K): Promise<void> {

        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";

        if (!href) {
            throw new Error("No href found or empty hrefs in object list");
        }
        const hrefs = nextCollection.map(o => o._links ? apiHelper.removeParamsFromNavigationHref(o._links.self) : "");
        if (!hrefs || hrefs.filter(h => !h).length > 0) {
            throw new Error("No hrefs found or empty hrefs in object list");
        }

        const assocUrl = stringHelper.getStringWitoutTrailingSlash(href) + "/" + property;
        const associationResponse = await this.adaptAnyToMany(assocUrl, hrefs);
        if (!associationResponse.ok) {
            throw new Error(associationResponse.statusText + ` (${associationResponse.status})`);
        }
    }

    public async moveObjectUp<T extends ApiBase, TMove extends ApiBase>(objToBeMovedUp: TMove, objectWithCollection: T, collectionPropertyName: keyof T): Promise<void> {

        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";
        const hrefMoveUp = objToBeMovedUp._links ? apiHelper.removeParamsFromNavigationHref(objToBeMovedUp._links.self) : "";

        if (!href) {
            throw new Error("No href found");
        }

        if (!hrefMoveUp) {
            throw new Error("No href found to move up");
        }

        const assocUrl = stringHelper.getStringWitoutTrailingSlash(href) + "/" + collectionPropertyName;

        let index = -1;
        const uris: string[] = ((objectWithCollection[collectionPropertyName] as any) as ApiBase[])
            .map((o, i) => {
                if (o.id === objToBeMovedUp.id) {
                    index = i
                }
                return !!o._links && apiHelper.removeParamsFromNavigationHref(o._links.self)
            }) as string[];
        if (index === -1) {
            throw new Error("Failed to find entry with matching href");
        }
        // swap index-1 with index to move task up:
        [uris[index - 1], uris[index]] = [uris[index], uris[index - 1]];
        this.adaptAnyToMany(assocUrl, uris)
    }

    public async moveObjectDown<T extends ApiBase, TMove extends ApiBase>(objToBeMovedUp: TMove, objectWithCollection: T, collectionPropertyName: keyof T): Promise<void> {

        const href = objectWithCollection._links ? apiHelper.removeParamsFromNavigationHref(objectWithCollection._links.self) : "";
        const hrefMoveDown = objToBeMovedUp._links ? apiHelper.removeParamsFromNavigationHref(objToBeMovedUp._links.self) : "";

        if (!href) {
            throw new Error("No href found");
        }

        if (!hrefMoveDown) {
            throw new Error("No href found to move down");
        }

        const assocUrl = stringHelper.getStringWitoutTrailingSlash(href) + "/" + collectionPropertyName;

        let index = -1;
        const uris: string[] = ((objectWithCollection[collectionPropertyName] as any) as ApiBase[])
            .map((o, i) => {
                if (o.id === objToBeMovedUp.id) {
                    index = i
                }
                return !!o._links && apiHelper.removeParamsFromNavigationHref(o._links.self)
            }) as string[];
        if (index === -1) {
            throw new Error("Failed to find entry with matching href");
        }
        // swap index-1 with index to move task up:
        [uris[index], uris[index + 1]] = [uris[index + 1], uris[index]];
        this.adaptAnyToMany(assocUrl, uris)
    }

    // TODO: we might get better results by even more generic parameters
    public async getObjectsCollection<T extends ApiBase, K extends keyof T & keyof ApiNavigationLinks>(obj: T, collectionPropertyName: K, embeddedName: string): Promise<T[K]> {
        if (!obj._links) {
            throw new Error("No links in object");
        }

        const link = obj._links[collectionPropertyName];
        if (!link || !link.href) {
            throw new Error("No href found in link property");
        }

        const response = await this.getRequest().get(apiHelper.removeParamsFromNavigationHref(link));

        if (!response.ok) {
            throw response;
        }

        const result: T[K] = ((await response.json()) as ApiHateoasObjectBase<T[K]>)._embedded[embeddedName];
        return result;
    }

    public async getObjectsReference<T extends ApiBase, K extends keyof T & keyof ApiNavigationLinks>(obj: T, collectionPropertyName: K, embeddedName: string | undefined): Promise<T[K]> {
        if (!obj._links) {
            throw new Error("No links in object");
        }

        const link = obj._links[collectionPropertyName];
        if (!link || !link.href) {
            throw new Error("No href found in link property");
        }

        const response = await this.getRequest().get(apiHelper.removeParamsFromNavigationHref(link));

        if (!response.ok) {
            throw response;
        }

        const result: T[K] = embeddedName ? ((await response.json()) as ApiHateoasObjectBase<T[K]>)._embedded[embeddedName] : ((await response.json()) as T[K]);
        return result;
    }

    public async createObjectAndAddWithAssociation<TNew extends ApiBase, T extends ApiBase, K extends keyof T["_links"]>
    (newObject: TNew, objectWithCollection: T, collectionPropertyName: K, createURI: string): Promise<TNew> {

        const href = apiHelper.removeParamsFromNavigationHref((apiHelper.ensureObjectHasLinks(objectWithCollection))._links.self);
        if (!href) {
            throw new Error("No self-href found for object");
        }

        const storedEntity: TNew & { _links: ApiNavigationLinks } = await this.createObject(newObject, createURI);
        await this.addToObj(storedEntity, objectWithCollection, collectionPropertyName);
        return apiHelper.injectIds(storedEntity);
    }

    public async deleteObjectAndAssociation<TDelete extends ApiBase, T extends ApiBase, K extends keyof T>
    (objectToDelete: TDelete, objectWithCollection: T, collectionPropertyName: K): Promise<void> {

        const href = apiHelper.removeParamsFromNavigationHref((apiHelper.ensureObjectHasLinks(objectWithCollection))._links.self);
        if (!href) {
            throw new Error("No self-href found for object");
        }

        await this.removeObjectFromCollection(objectToDelete, objectWithCollection, collectionPropertyName);
        await this.deleteObject(objectToDelete);
    }
}