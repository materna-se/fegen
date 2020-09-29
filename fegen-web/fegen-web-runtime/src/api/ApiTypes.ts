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

export interface Dto {
    _links: ApiNavigationLinks
}

export interface Entity extends Dto {
    id: number
}

export type WithId<D> = D & { id: number };

export interface ApiNavigationLinks {
    self: ApiNavigationLink
    [key: string]: ApiNavigationLink
}

export interface ApiNavigationLink {
    href: string;
    templated?: boolean;
}

export interface ApiHateoasObjectBase<T> {
    _embedded: {
        [key: string]: T
    }
    _links: ApiNavigationLinks;
}
export interface ApiHateoasObjectReadMultiple<T> {
    _embedded: {
        [key: string]: T
    }
    _links: ApiNavigationLinks;
    page: PageData;
}
export interface PageData {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
}
export interface PageLinks extends ApiNavigationLinks {
    profile: ApiNavigationLink;
}
export interface Items<T> {
    items: T[];
    _links: ApiNavigationLinks;
}
export interface PagedItems<T> extends Items<T>{
    page: PageData;
}
