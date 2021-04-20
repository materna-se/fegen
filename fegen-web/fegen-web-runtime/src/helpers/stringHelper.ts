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
export class StringHelper {

    constructor() {
        this.generateGuid = this.generateGuid.bind(this);
    }

    public appendParams(url: string, parameters: {[key: string]: string | number | boolean | undefined}): string {
        let result = url;
        let separator = "?";
        for (const key in parameters) {
            const value = parameters[key];
            if (value !== undefined) {
                result += separator + key + "=" + encodeURIComponent(value.toString());
                separator = "&"
            }
        }
        return result;
    }

    public generateGuid(): string {

        const first = StringHelper.getStringWithFourHexValues() + StringHelper.getStringWithFourHexValues();
        const sec = StringHelper.getStringWithFourHexValues();
        const third = StringHelper.getStringWithFourHexValues();
        const fourth = StringHelper.getStringWithFourHexValues();
        const fifth = StringHelper.getStringWithFourHexValues() + StringHelper.getStringWithFourHexValues() + StringHelper.getStringWithFourHexValues();

        return (first + "-" + sec + "-" + third + "-" + fourth + "-" + fifth);
    }

    public getStringWithoutTrailingSlash(url: string) {

        let subResult = url;

        while (subResult && subResult.lastIndexOf("/") === subResult.length - 1) {
            subResult = subResult.substr(0, subResult.length - 1);
        }
        return subResult;
    }

    public createCssUrlString(url: string): string | undefined {
        if (!url) { return undefined; }
        return `url(${url})`
    }

    private static getStringWithFourHexValues(): string {
        // tslint:disable-next-line:no-bitwise
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }
}

export default new StringHelper();
