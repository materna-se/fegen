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
import {ApiNavigationLink, Dto, WithId} from './ApiTypes';

class ApiHelper {

    constructor() {
        this.removeParamsFromNavigationHref = this.removeParamsFromNavigationHref.bind(this);
        this.getObjectId = this.getObjectId.bind(this);
        this.getIdFromHref = this.getIdFromHref.bind(this);
        this.getSelfLink = this.getSelfLink.bind(this);
        this.injectIdsInObjectAndProperties = this.injectIdsInObjectAndProperties.bind(this);
        this.injectIds = this.injectIds.bind(this);
        this.objectHasSelfLink = this.objectHasSelfLink.bind(this);
    }

    public removeParamsFromNavigationHref(navLink: ApiNavigationLink): string {
        if (navLink &&
            (navLink.templated)) {
            let href = navLink.href;
            const reg = new RegExp("{[^{}]*}$", "g");
            const match = reg.exec(href);
            href = match ? href.substr(0, match.index) : href;
            return href;
        }
        return navLink ? navLink.href : "";
    }


    public getObjectId(obj: Dto) { return (this.getIdFromHref(this.getSelfLink(obj)) || -1) };

    public getIdFromHref(href: string | undefined) {
        // TODO work with {projection} (templated uris)
        const lastIndex = href ? href.lastIndexOf("/") : -1;
        const indexOfBraces = href ? href.indexOf("{", lastIndex) : -1;
        const length = indexOfBraces >= 0 ? indexOfBraces - lastIndex : undefined;
        return href && lastIndex >= 0 ? +(length ? href.substr(lastIndex + 1, length - 1) : href.substr(lastIndex + 1)) : undefined
    }
    public getSelfLink(obj: any) {
        return (obj && obj._links && obj._links.self && obj._links.self.href ?
            obj._links.self.href :
            undefined);
    }

    public injectIdsInObjectAndProperties<T extends object>(obj: T): T {
        if (typeof obj !== "object") {
            return obj;
        }

        const result: T = {
            // Any due to cast of generic is forbidden in 3.1.4
            ...(obj as any),
        };

        for (const prop in obj) {
            if(obj.hasOwnProperty(prop)){
                const value = obj[prop];
                if (this.objectHasSelfLink(value)) {
                    result[prop] = this.injectIds(value as typeof value & Dto);
                } else if (!!value && value instanceof Array) {
                    result[prop] = value.map(v => {
                        return this.injectIdsInObjectAndProperties(v as Dto);
                    }) as typeof value;
                } else if (!!value && typeof value === "object") {
                    // TODO: #FIXME
                    result[prop] = this.injectIdsInObjectAndProperties(value as any);
                }
            }
        }

        return (this.objectHasSelfLink(result)) ?
            {
                ...(result as any),
                id: this.getObjectId(obj as T & Dto),
            } :
            result;
    }

    public injectIds<D extends Dto>(obj: D): WithId<D> {
        const result: D = {
            // Any due to cast of generic is forbidden in 3.1.4
            ...(obj as any),
        };

        for (const prop in obj) {
            if(obj.hasOwnProperty(prop)){
                const value = obj[prop];
    
                if (this.objectHasSelfLink(value)) {
                    result[prop] = this.injectIds(value as typeof value & Dto);
                } else if (!!value && value instanceof Array) {
                    result[prop] = value.map(v => {
                        return this.injectIdsInObjectAndProperties(v);
                    }) as typeof value;
                }
            
            }
        }

        return {
            ...(result as any),
            id: this.getObjectId(obj),
        };
    }

    public objectHasSelfLink(obj: any): boolean {
        return (!!obj && !!obj._links);
    }
}
export default new ApiHelper();