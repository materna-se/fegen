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
import  RequestAdapter from "./RequestAdapter"
import {ApiBase, Dto, PagedItems, WithId} from "./ApiTypes";
import {apiHelper} from "../index";

export abstract class BaseClient<C, B extends ApiBase> {
    // tslint:disable-next-line:variable-name
    protected _requestAdapter: RequestAdapter = new RequestAdapter();
    // tslint:disable-next-line:variable-name
    protected _apiClient: C;
    // tslint:disable-next-line:variable-name
    protected _uri: string;
    // tslint:disable-next-line:variable-name
    protected _embeddedPropName: string;

    constructor(uri: string, embeddedPropName: string, apiClient: C, requestAdapter?: RequestAdapter) {
        this._requestAdapter = requestAdapter || this._requestAdapter;
        this._apiClient = apiClient;
        this._uri = uri;
        this._embeddedPropName = embeddedPropName
    }

    protected static replaceEntitiesWithLinks<T>(obj: T): {} {
        const result = Object.assign({}, obj);
        for (const prop in result) {
            if (!result[prop]) {
                continue;
            }
            if (result[prop]["_links"]) {
                result[prop] = result[prop]["_links"]["self"]["href"];
            } else if (result[prop] instanceof Array) {
                // @ts-ignore
                result[prop] = (result[prop] as unknown as Array<{}>).map((item: unknown) => {
                    if (item instanceof Object && item["_links"]) {
                        return item["_links"]["self"]["href"];
                    } else {
                        return item;
                    }
                });
            }
        }
        return result;
    }

    public async create(obj: Partial<B>): Promise<WithId<B>> {
        const requestObj = BaseClient.replaceEntitiesWithLinks(obj);
        return await this._requestAdapter.createObject(requestObj, this._uri) as WithId<B>;
    }

    public async readProjections<T extends Dto<B>>(projectionName?: string, page?: number, size?: number, sort?: string) : Promise<PagedItems<T>> {
        return await this._requestAdapter.getPage<T>(this._uri, this._embeddedPropName, projectionName, page, size, sort);
    }

    public async readOne(id: number): Promise<WithId<B> | undefined> {
        return this.readProjection<WithId<B>>(id);
    }

    public async readProjection<T extends WithId<B>>(id: number, projection?: string): Promise<T | undefined> {
        const hasProjection = (!!projection);
        let fullUrl = `${this._uri}/${id}`;
        fullUrl = hasProjection ? `${fullUrl}?projection=${projection}` : fullUrl;

        const response =  await this._requestAdapter.getRequest().get(fullUrl);
        if(response.status === 404) return undefined;
        if(!response.ok){ throw response; }

        const obj = (await response.json()) as T;
        return apiHelper.injectIds(obj);
    }

    public async update<T extends WithId<B>>(obj: T): Promise<WithId<B>> {
        const apiItem: WithId<B> = this.toDefaultType(obj);
        return await this._requestAdapter.updateObject(apiItem)
    }

    public toDefaultType<T extends WithId<B>>(obj: T): WithId<B> {
        return Object.assign({id: obj.id, _links: obj._links}, this.toBase(obj));
    }

    public abstract toDto(obj: WithId<B>): Dto<B>;

    public abstract toBase<T extends B>(obj: T): B;

    public async delete(obj: Dto<B>) {
        return await this._requestAdapter.deleteObject(obj)
    }
}