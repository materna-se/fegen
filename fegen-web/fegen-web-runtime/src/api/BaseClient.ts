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
import {Dto, PagedItems, WithId} from "./ApiTypes";
import {apiHelper} from "../index";

export abstract class BaseClient<C, N, D extends Dto> {

    // tslint:disable-next-line:variable-name
    protected _requestAdapter: RequestAdapter = new RequestAdapter();
    // tslint:disable-next-line:variable-name
    protected _apiClient: C;
    // tslint:disable-next-line:variable-name
    protected _uri: string;
    // tslint:disable-next-line:variable-name
    protected _embeddedPropName: string;

    protected constructor(uri: string, embeddedPropName: string, apiClient: C, requestAdapter?: RequestAdapter) {
        this._requestAdapter = requestAdapter || this._requestAdapter;
        this._apiClient = apiClient;
        this._uri = uri;
        this._embeddedPropName = embeddedPropName
    }

    protected static replaceEntitiesWithLinks<T>(obj: T): {} {
        const result = Object.assign({}, obj);
        for (const prop in result) {
            if (!result.hasOwnProperty(prop)) {
                break;
            }
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

    public async create(obj: N): Promise<WithId<D>> {
        return this.createWithDefaults(obj);
    }

    /**
     * Creates an entity and uses default values for fields that are not included in obj.
     * Default values do not exist for every field (e.g. not for NotNull entities), so take care when using this method.
     */
    public async createWithDefaults(obj: Partial<N>): Promise<WithId<D>> {
        const requestObj = BaseClient.replaceEntitiesWithLinks(obj);
        return await this._requestAdapter.createObject<D>(requestObj, this._uri);
    }

    public async readProjections<P extends WithId<D>>(projectionName?: string, page?: number, size?: number, sort?: string) : Promise<PagedItems<P>> {
        return await this._requestAdapter.getPage<P>(this._uri, this._embeddedPropName, projectionName, page, size, sort);
    }

    public async readOne(id: number): Promise<WithId<D> | undefined> {
        return this.readProjection<WithId<D>>(id);
    }

    public async readProjection<P extends WithId<D>>(id: number, projection?: string): Promise<P | undefined> {
        const hasProjection = (!!projection);
        let fullUrl = `${this._uri}/${id}`;
        fullUrl = hasProjection ? `${fullUrl}?projection=${projection}` : fullUrl;

        const response =  await this._requestAdapter.getRequest().get(fullUrl);
        if(response.status === 404) return undefined;
        if(!response.ok){ throw response; }

        const obj = (await response.json()) as P;
        return apiHelper.injectIds(obj);
    }

    /**
     * Returns a new object that only contains the fields of WithId<D>.
     * This may be necessary when updating an entity using a projection since
     * sending a PUT request with e.g. relationship fields causes errors.
     */
    protected abstract toPlainObj<T extends WithId<D>>(obj: T): WithId<D>;

    public async update<T extends WithId<D>>(obj: T): Promise<WithId<D>> {
        const request = this.toPlainObj(obj);
        return await this._requestAdapter.updateObject(request);
    }

    public async delete(obj: WithId<D>) {
        return await this._requestAdapter.deleteObject(obj)
    }
}